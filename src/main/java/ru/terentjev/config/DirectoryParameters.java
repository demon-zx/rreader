package ru.terentjev.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ConfigurationProperties(prefix = "application.path")
public record DirectoryParameters(Path config, Path books, Path pages) {
    private static final Logger log = LoggerFactory.getLogger(DirectoryParameters.class);

    public DirectoryParameters {
        try {
            checkAndCreate(config);
            checkAndCreate(books);
            checkAndCreate(pages);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkAndCreate(Path path) throws IOException {
        log.info("Check directory: {}", path);
        if (Files.notExists(path)) {
            log.info("Create directory: {}", path);
            Files.createDirectories(path);
        }
    }
}
