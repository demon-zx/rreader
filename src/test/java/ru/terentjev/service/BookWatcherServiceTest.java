package ru.terentjev.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.config.WatchParameters;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.render.RenderService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookWatcherServiceTest {

    private static final Duration DEBOUNCE = Duration.ofMillis(200);

    @TempDir
    Path tempDir;

    private BookWatcherService service;
    private BookUploadService uploadService;
    private BookService bookService;
    private BookmarkService bookmarkService;
    private RenderService renderService;
    private Path books;

    @BeforeEach
    void setUp() throws IOException {
        uploadService = mock(BookUploadService.class);
        bookService = mock(BookService.class);
        bookmarkService = mock(BookmarkService.class);
        renderService = mock(RenderService.class);

        DirectoryParameters directories = new DirectoryParameters(
                tempDir.resolve("config"),
                tempDir.resolve("books"),
                tempDir.resolve("pages")
        );
        books = directories.books();
        service = new BookWatcherService(
                directories,
                new WatchParameters(true, DEBOUNCE),
                uploadService,
                bookService,
                bookmarkService,
                renderService
        );
        service.firstRun();
        Thread poller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                service.watch();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "book-watcher-test-poller");
        poller.setDaemon(true);
        poller.start();
    }

    @Test
    void createFileTriggersProcessing() throws Exception {
        Path file = Files.writeString(books.resolve("test.fb2"), "content");

        await(() -> verify(uploadService, atLeastOnce()).process(argThat(
                p -> p != null && p.endsWith("test.fb2")
        )));
        Files.delete(file);
    }

    @Test
    void modifyFileTriggersReprocessing() throws Exception {
        Path file = Files.writeString(books.resolve("test.fb2"), "content v1");
        await(() -> verify(uploadService, atLeastOnce()).process(any(Path.class)));

        Thread.sleep(DEBOUNCE.toMillis() * 2);
        Files.writeString(file, "content v2");

        await(() -> verify(uploadService, org.mockito.Mockito.times(2)).process(any(Path.class)));
    }

    @Test
    void deleteFileTriggersBookDeletion() throws Exception {
        Path file = Files.writeString(books.resolve("test.fb2"), "content");
        Book book = book(file.toUri());
        when(bookService.findByUriContaining(file.toUri().toString())).thenReturn(List.of(book));

        await(() -> verify(uploadService, atLeastOnce()).process(any(Path.class)));
        Files.delete(file);

        await(() -> {
            verify(bookmarkService).deleteByBook(book.getId());
            verify(renderService).deleteCache(book.getId());
            verify(bookService).delete(book.getId());
        });
    }

    @Test
    void deleteDirectoryTriggersAllInnerBooksDeletion() throws Exception {
        Path directory = Files.createDirectories(books.resolve("series"));
        Book plain = book(books.resolve("series/1.fb2").toUri());
        Book zipped = book(URI.create("zip:" + books.resolve("series/1.zip").toUri() + "!/1.fb2"));
        when(bookService.findByUriContaining(anyString())).thenAnswer(invocation -> {
            String search = invocation.getArgument(0);
            return search.contains("series") ? List.of(plain, zipped) : List.of();
        });

        Files.delete(directory);

        await(() -> {
            verify(bookService).delete(plain.getId());
            verify(bookService).delete(zipped.getId());
        });
    }

    private static Book book(URI uri) {
        return new Book(
                UUID.randomUUID(),
                "Title",
                "Author",
                null,
                null,
                null,
                "FB2",
                uri,
                "checksum",
                null,
                100
        );
    }

    @FunctionalInterface
    private interface Assertion {
        void run() throws Exception;
    }

    private static void await(Assertion assertion) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        Throwable last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                assertion.run();
                return;
            } catch (AssertionError | Exception e) {
                last = e;
                Thread.sleep(100);
            }
        }
        throw new AssertionError("Condition not met in time", last);
    }
}
