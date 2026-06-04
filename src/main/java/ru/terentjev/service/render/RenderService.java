package ru.terentjev.service.render;

import org.apache.commons.lang3.function.FailableConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.service.BookService;
import ru.terentjev.service.model.Book;
import ru.terentjev.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class RenderService {
    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

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

    public void renderCover(
            UUID id,
            FailableConsumer<InputStream, IOException> mapper
    ) throws IOException, InvalidArgumentException {
        Book book = service.findById(id);
        Path rendered = path(id).resolve("cover.png");
        render(1, rendered, book);
        out(mapper, rendered);
    }

    public void renderPage(
            UUID id,
            int page,
            FailableConsumer<InputStream, IOException> mapper
    ) throws IOException, InvalidArgumentException {
        Book book = service.findById(id);
        Path rendered = path(id).resolve(page + ".png");
        render(page, rendered, book);
        out(mapper, rendered);
    }

    private void render(int page, Path file, Book book) throws IOException {
        if (Files.notExists(file)) {
            if (Files.notExists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            IOUtil.readFile(
                    book.getPath(),
                    is -> {
                        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
                            Render render = renderFactory.getRender(book.getType());
                            render.render(page, is, os);
                            os.flush();
                        }
                        return null;
                    }
            );
        }
    }

    private Path path(UUID id) {
        return Path.of("pages", id.toString());
    }
}
