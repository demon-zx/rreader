package ru.terentjev.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BookUploadService {
    private static final Logger log = LoggerFactory.getLogger(BookUploadService.class);

    private final Path pathBooks;

    public BookUploadService(Path pathBooks) {
        this.pathBooks = pathBooks;
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
        }
    }
}
