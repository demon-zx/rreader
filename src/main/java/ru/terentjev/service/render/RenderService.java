package ru.terentjev.service.render;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.springframework.stereotype.Service;

import ru.terentjev.exception.ServiceException;
import ru.terentjev.service.BookService;
import ru.terentjev.service.model.Book;
import ru.terentjev.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class RenderService {

    private final BookService service;
    private final RenderFactory renderFactory;

    public RenderService(BookService service, RenderFactory renderFactory) {
        this.service = service;
        this.renderFactory = renderFactory;
    }

    private void out(FailableConsumer<InputStream, IOException> mapper, Path fileForOut) throws IOException {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(fileForOut))) {
            mapper.accept(is);
        }
    }

    private RenderSettings settings(UUID deviceUuid, int width, int heigth) throws ServiceException {
        return new RenderSettings(width, heigth);
    }

    public OpenInfo open(UUID id, UUID deviceUuid, int width, int height) throws ServiceException, IOException {
        Book book = service.findById(id);
        Render render = renderFactory.getRender(book.getType());
        var settings = settings(deviceUuid, width, height);
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
        var settings = settings(deviceUuid, width, height);
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
        return Path.of("pages", id.toString());
    }

    private Path path(UUID id, UUID deviceUuid, int widht, int height) {
        return Path.of("pages", id.toString(), deviceUuid.toString(), widht + "x" + height);
    }
}
