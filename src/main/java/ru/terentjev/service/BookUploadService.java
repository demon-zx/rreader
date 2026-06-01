package ru.terentjev.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.service.model.Book;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

@Service
public class BookUploadService {
    private static final Logger log = LoggerFactory.getLogger(BookUploadService.class);

    private final Path pathBooks;
    private final Clock clock;
    private final BookService bookService;

    public BookUploadService(DirectoryParameters directoryParameters, Clock clock, BookService bookService) {
        this.pathBooks = directoryParameters.books();
        this.clock = clock;
        this.bookService = bookService;
    }

    public void scan() {
        log.info("Scanning books...");
        try (var filesStream = Files.walk(pathBooks)) {
            filesStream.filter(Files::isRegularFile)
                    .forEach(this::process);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void process(Path file) {
        log.info("Find file: {}", file);
        String path = file.toAbsolutePath()
                .toString();
        Optional<Book> exist = bookService.findByPath(path);
        if (exist.isEmpty()) {
            register(file, path);
        }
    }

    private void register(Path file, String path) {
        log.info("Register file: {}", file);
        Path filename = file.getFileName();
        String type = getType(filename);
        Book book = new Book(
                null,
                filename.toString(),
                null,
                null,
                type,
                path,
                "-",
                null
        );
        bookService.save(book);
    }

    private String getType(Path filename) {
        return StringUtils.substringAfterLast(filename.toString(), ".");
    }


}
