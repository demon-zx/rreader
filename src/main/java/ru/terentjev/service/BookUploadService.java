package ru.terentjev.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.Render;
import ru.terentjev.service.render.RenderFactory;
import ru.terentjev.util.IOUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class BookUploadService {
    private static final Logger log = LoggerFactory.getLogger(BookUploadService.class);

    private final Path pathBooks;
    private final BookService bookService;
    private final RenderFactory renderFactory;

    public BookUploadService(
            DirectoryParameters directoryParameters,
            BookService bookService,
            RenderFactory renderFactory
    ) {
        this.pathBooks = directoryParameters.books();
        this.bookService = bookService;
        this.renderFactory = renderFactory;
    }

    public void scan() {
        log.info("Scanning books...");
        try (var filesStream = Files.walk(pathBooks)) {
            List<Path> files = filesStream.filter(Files::isRegularFile)
                    .toList();
            for (Path file : files) {
                process(file);
            }
            log.info("Scanning complete.");
        } catch (IOException e) {
            log.error("Scanning fail.", e);
        }
    }

    private void process(Path file) throws IOException {
        log.info("Find file: {}", file);
        String path = file.toAbsolutePath()
                .toString();
        Optional<Book> exist = bookService.findByPath(path);
        if (exist.isEmpty()) {
            register(file, path);
        }
    }

    private void register(Path file, String path) throws IOException {
        Path name = file.getFileName();
        String type = getType(name);
        Info info = info(file, type);
        Book book = new Book(
                null,
                info.getTitle(),
                info.getAuthor(),
                null,
                type,
                path,
                checksum(file),
                null,
                Files.size(file)
        );
        log.info("Register file ({}): {}", book.getChecksum(), file);
        bookService.save(book);
    }

    private String getType(Path filename) {
        return StringUtils.substringAfterLast(filename.toString(), ".")
                .toUpperCase();
    }

    private Info info(Path file, String type) throws IOException {
        return IOUtil.readFile(
                file, is -> {
                    Render render = renderFactory.getRender(type);
                    return render.info(is);
                }
        );
    }

    private String checksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return IOUtil.readFile(
                    file,
                    is -> {
                        byte[] buffer = new byte[1024];
                        int rb;
                        while ((rb = is.read(buffer)) > 0) {
                            digest.update(buffer, 0, rb);
                        }
                        return HexFormat.of()
                                .formatHex(digest.digest());
                    }
            );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
