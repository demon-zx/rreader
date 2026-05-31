package ru.terentjev.service;

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
import java.util.List;
import java.util.UUID;

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
        List<Path> list;
        try (var files = Files.walk(pathBooks)) {
            list = files.filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Path path : list) {
            log.info("File: {}", path);
            Book book = new Book(
                    null,
                    path.getFileName().toString(),
                    null,
                    null,
                    "pdf",
                    path.toString(),
                    "-",
                    null
            );
            bookService.save(book);
        }
    }
}
