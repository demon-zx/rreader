package ru.terentjev.util;

import org.apache.commons.lang3.function.FailableFunction;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IOUtil {

    private IOUtil() {}

    public static <T> T readFile(Path file, FailableFunction<InputStream, T, IOException> mapper) throws IOException {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
            return mapper.apply(is);
        }
    }

}
