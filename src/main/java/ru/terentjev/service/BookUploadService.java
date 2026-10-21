package ru.terentjev.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.ProcessResult;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.Render;
import ru.terentjev.service.render.RenderFactory;
import ru.terentjev.util.IOUtil;
import ru.terentjev.util.fs.FileSystems;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Order(1)
public class BookUploadService implements FirstRunService {

    private static final Logger log = LoggerFactory.getLogger(BookUploadService.class);
    private final Path pathBooks;
    private final BookService bookService;
    private final ProcessedFileService processedFileService;
    private final RenderFactory renderFactory;

    public BookUploadService(
            DirectoryParameters directoryParameters,
            BookService bookService,
            ProcessedFileService processedFileService,
            RenderFactory renderFactory
    ) throws IOException {
        this.pathBooks = directoryParameters.books();
        this.bookService = bookService;
        this.processedFileService = processedFileService;
        this.renderFactory = renderFactory;
    }

    private void walk(Path path, FailableConsumer<Path, IOException> fileConsumer) {
        log.info("Scanning books in {}.", path);
        try (var filesStream = Files.walk(path)) {
            List<Path> files = filesStream.filter(Files::isRegularFile)
                    .toList();
            for (Path file : files) {
                fileConsumer.accept(file);
            }
            log.info("Scanning {} complete.", path);
        } catch (IOException e) {
            log.error("Scanning {} fail.", path, e);
        }
    }

    public void processDirectory(Path path) {
        walk(path, this::process);
    }

    public void process(Path file) {
        file = file.toAbsolutePath();
        log.info("Find file: {}", file);
        try {
            if (!processedFileService.isChanged(file)) {
                log.debug("Skip unchanged file: {}", file);
                return;
            }
            boolean archive = file.toString()
                    .toLowerCase()
                    .endsWith(".zip");
            if (archive) {
                processArchive(file);
            } else {
                register(file);
            }
            processedFileService.markProcessed(
                    file,
                    ProcessResult.SUCCESS,
                    null,
                    Files.size(file),
                    modifiedAt(file)
            );
        } catch (Exception e) {
            log.error("Process {} fail.", file, e);
            processedFileService.markProcessed(
                    file,
                    ProcessResult.ERROR,
                    StringUtils.abbreviate(String.valueOf(e.getMessage()), 4096),
                    null,
                    null
            );
        }
    }

    private void processArchive(Path archive) throws IOException {
        log.info("Scan archive.");
        try (var fs = FileSystems.newFileSystem(archive)) {
            walk(fs.getPath("/"), file -> register(file));
        }
        log.info("Scan archive complete.");
    }

    private void register(Path file) throws IOException {
        Path name = file.getFileName();
        String type = getType(name);
        Info info = info(file, type);
        URI uri = file.toUri();
        UUID id = bookService.findByUri(uri)
                .map(Book::getId)
                .orElse(null);
        Book book = new Book(
                id,
                Objects.requireNonNullElse(info.getTitle(), name.toString()),
                Objects.requireNonNullElse(info.getAuthor(), "N/A"),
                null,
                info.getSequenceName(),
                info.getSequenceNumber(),
                type,
                uri,
                checksum(file),
                null,
                Files.size(file)
        );
        log.info("{} file ({}): {}", id == null ? "Register" : "Update", book.getChecksum(), file);
        bookService.save(book);
    }

    private String getType(Path filename) {
        return StringUtils.substringAfterLast(filename.toString(), ".")
                .toUpperCase();
    }

    private Info info(Path file, String type) throws IOException {
        return IOUtil.readFile(file, is -> {
            Render render = renderFactory.getRender(type);
            return render.info(is);
        });
    }

    private String checksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return IOUtil.readFile(file, is -> {
                byte[] buffer = new byte[1024];
                int rb;
                while ((rb = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, rb);
                }
                return HexFormat.of()
                        .formatHex(digest.digest());
            });
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Instant modifiedAt(Path file) throws IOException {
        return Files.getLastModifiedTime(file).toInstant();
    }

    @Override
    public void firstRun() {
        log.info("Start scan books...");
        walk(pathBooks, file -> process(file));
        log.info("Stop all scan books.");
    }

}
