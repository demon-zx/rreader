package ru.terentjev.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.config.WatchParameters;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.render.RenderService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Order(2)
public class BookWatcherService implements FirstRunService {

    private static final Logger log = LoggerFactory.getLogger(BookWatcherService.class);

    private final DirectoryParameters directories;
    private final WatchParameters parameters;
    private final BookUploadService uploadService;
    private final BookService bookService;
    private final BookmarkService bookmarkService;
    private final RenderService renderService;

    private WatchService watchService;
    private ScheduledExecutorService executor;
    private final Map<Path, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

    public BookWatcherService(
            DirectoryParameters directories,
            WatchParameters parameters,
            BookUploadService uploadService,
            BookService bookService,
            BookmarkService bookmarkService,
            RenderService renderService
    ) {
        this.directories = directories;
        this.parameters = parameters;
        this.uploadService = uploadService;
        this.bookService = bookService;
        this.bookmarkService = bookmarkService;
        this.renderService = renderService;
    }

    @Override
    public void firstRun() {
        if (!parameters.enabled()) {
            log.info("Book watcher disabled.");
            return;
        }
        try {
            watchService = FileSystems.getDefault().newWatchService();
            executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "book-watcher");
                thread.setDaemon(true);
                return thread;
            });
            registerRecursive(directories.books());
            log.info("Book watcher started on {}.", directories.books());
        } catch (IOException e) {
            close();
            throw new UncheckedIOException("Book watcher start fail.", e);
        }
    }

    /**
     * Неблокирующая выборка событий файловой системы по расписанию Spring.
     */
    @Scheduled(fixedDelayString = "${application.watch.poll-interval:100}")
    public void watch() {
        if (watchService == null) {
            return;
        }
        WatchKey key = watchService.poll();
        if (key == null) {
            return;
        }
        Path directory = (Path) key.watchable();
        for (WatchEvent<?> event : key.pollEvents()) {
            handle(directory, event);
        }
        key.reset();
    }

    private void registerRecursive(Path parentDirectory) throws IOException {
        Files.walkFileTree(parentDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                log.info("Watch directory: {}", directory);
                directory.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void handle(Path directory, WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            log.warn("Book watcher events overflow, some changes may be missed.");
            return;
        }
        Path path = directory.resolve((Path) event.context());
        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
            try {
                registerRecursive(path);
                uploadService.processDirectory(path);
            } catch (IOException e) {
                log.error("Register directory {} fail.", path, e);
            }
            return;
        }
        debounce(path, () -> process(path));
    }

    private void debounce(Path path, Runnable task) {
        pending.compute(path, (key, previous) -> {
            if (previous != null) {
                previous.cancel(false);
            }
            return executor.schedule(task, parameters.debounce().toMillis(), TimeUnit.MILLISECONDS);
        });
    }

    private void process(Path file) {
        pending.compute(file, (key, scheduled) -> {
            try {
                if (Files.notExists(file)) {
                    deleteBooks(file);
                } else {
                    if (Files.isRegularFile(file)) {
                        uploadService.process(file);
                    }
                }
            } catch (Exception e) {
                log.error("Process {} fail.", file, e);
            }
            return null;
        });
    }

    private void deleteBooks(Path path) throws IOException {
        List<Book> removed = bookService.findByUriContaining(path.toUri().toString());
        for (Book book : removed) {
            bookmarkService.deleteByBook(book.getId());
            renderService.deleteCache(book.getId());
            bookService.delete(book.getId());
        }
        if (!removed.isEmpty()) {
            log.info("Deleted {} book(s) for removed path {}.", removed.size(), path);
        }
    }

    private void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Watch service close fail.", e);
            }
        }
    }

}
