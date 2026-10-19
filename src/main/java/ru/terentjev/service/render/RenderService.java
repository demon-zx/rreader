package ru.terentjev.service.render;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.exception.ServiceException;
import ru.terentjev.service.BookService;
import ru.terentjev.service.SettingsChangedEvent;
import ru.terentjev.service.SettingsService;
import ru.terentjev.service.mapper.SettingsMapper;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.Settings;
import ru.terentjev.service.model.SettingsScope;
import ru.terentjev.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class RenderService {

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    private final BookService service;
    private final RenderFactory renderFactory;
    private final SettingsService settingsService;
    private final DirectoryParameters directories;

    public RenderService(
            BookService service,
            RenderFactory renderFactory,
            SettingsService settingsService,
            DirectoryParameters directories
    ) {
        this.service = service;
        this.renderFactory = renderFactory;
        this.settingsService = settingsService;
        this.directories = directories;
    }

    private void out(FailableConsumer<InputStream, IOException> mapper, Path fileForOut) throws IOException {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(fileForOut))) {
            mapper.accept(is);
        }
    }

    private RenderSettings settings(Book book, UUID deviceUuid, int width, int height) throws ServiceException {
        Settings effective = settingsService.read(deviceUuid, scope(book.getType()));
        Map<String, String> values = new LinkedHashMap<>();
        for (KeyValue kv : effective.getSettings()) {
            values.put(kv.getKey(), kv.getValue());
        }
        return new RenderSettings(width, height, values);
    }

    private SettingsScope scope(String bookType) {
        return SettingsMapper.mapper.scope(bookType);
    }

    public OpenInfo open(UUID id, UUID deviceUuid, int width, int height) throws ServiceException, IOException {
        Book book = service.findById(id);
        Render render = renderFactory.getRender(book.getType());
        var settings = settings(book, deviceUuid, width, height);
        return file(is -> render.open(settings, is, path(id), path(id, deviceUuid, width, height)), book);
    }

    public void renderCover(
            UUID id,
            UUID deviceUuid,
            FailableConsumer<InputStream, IOException> mapper
    ) throws IOException, ServiceException {
        Book book = service.findById(id);
        Render render = renderFactory.getRender(book.getType());
        Path bookDirectory = path(id);
        Path rendered = bookDirectory.resolve("cover.png");
        render((is, os) -> render.cover(is, bookDirectory, os), rendered, book);
        out(mapper, rendered);
    }

    public void renderPage(
            UUID id,
            int page,
            UUID deviceUuid,
            int width, 
            int height,
            FailableConsumer<InputStream, IOException> mapper
    ) throws IOException, ServiceException {
        Book book = service.findById(id);
        Render render = renderFactory.getRender(book.getType());
        Path bookDirectory = path(id);
        Path deviceDirectory = path(id, deviceUuid, width, height);
        Path rendered = deviceDirectory.resolve(page + ".png");
        var settings = settings(book, deviceUuid, width, height);
        render((is, os) -> render.render(settings, page, is, bookDirectory, deviceDirectory, os), rendered, book);
        out(mapper, rendered);
    }

    private void render(
            FailableBiConsumer<InputStream, OutputStream, IOException> renderMethod,
            Path file,
            Book book
    ) throws IOException {
        if (Files.notExists(file)) {
            if (Files.notExists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            file(
                is -> {
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
                        renderMethod.accept(is, os);
                        os.flush();
                    }
                    return null;
                }, 
                book
            );
        }
    }

    private <T> T file(
            FailableFunction<InputStream, T, IOException> method,
            Book book
    ) throws IOException {
        return IOUtil.readFile(
                book.getUri(),
                method
        );
    }

    private Path path(UUID id) {
        return directories.pages().resolve(id.toString());
    }

    private Path path(UUID id, UUID deviceUuid, int widht, int height) {
        return directories.pages()
                .resolve(id.toString())
                .resolve(deviceUuid.toString())
                .resolve(widht + "x" + height);
    }

    @EventListener
    public void onSettingsChanged(SettingsChangedEvent event) throws IOException {
        Path root = Path.of("pages");
        if (Files.notExists(root)) {
            return;
        }
        try (Stream<Path> books = Files.list(root)) {
            for (Path book : books.toList()) {
                Path deviceDirectory = book.resolve(event.deviceUuid().toString());
                if (Files.exists(deviceDirectory)) {
                    deleteRecursively(deviceDirectory);
                }
            }
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete cached file {}", path, e);
                        }
                    });
        }
    }

}
