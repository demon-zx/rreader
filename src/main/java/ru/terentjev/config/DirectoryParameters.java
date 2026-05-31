package ru.terentjev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ConfigurationProperties(prefix = "application.path")
public record DirectoryParameters(Path config, Path books) {

    public DirectoryParameters {
        try {
            checkAndCreate(config);
            checkAndCreate(books);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkAndCreate(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }
}
