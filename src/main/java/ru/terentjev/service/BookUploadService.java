package ru.terentjev.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.service.model.Book;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        log.info("Scanning complete.");
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
        Path name = file.getFileName();
        String type = getType(name);
        Book book = new Book(
                null,
                name.toString(),
                null,
                null,
                type,
                path,
                checksum(file),
                null
        );
        log.info("Register file ({}): {}", book.getChecksum(), file);
        bookService.save(book);
    }

    private String getType(Path filename) {
        return StringUtils.substringAfterLast(filename.toString(), ".");
    }

    private String checksum(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
                int rb = is.read(buffer);
                digest.update(buffer, 0, rb);
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : digest.digest()) {
                builder.append(Integer.toHexString(b & 0xff));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
