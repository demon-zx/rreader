package ru.terentjev.util;

import org.apache.commons.lang3.function.FailableFunction;
import ru.terentjev.util.fs.FileSystems;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class IOUtil {

    private IOUtil() {}

    public static <T> T readFile(Path file, FailableFunction<InputStream, T, IOException> mapper) throws IOException {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
            return mapper.apply(is);
        }
    }

    public static <T> T readFile(URI file, FailableFunction<InputStream, T, IOException> mapper) throws IOException {
        String scheme = file.getScheme();
        if (!scheme.equals("file")) {
            try (FileSystem fs = FileSystems.newFileSystem(file)) {
                String part = file.getSchemeSpecificPart();
                return IOUtil.readFile(fs.getPath(part.split("!/")[1]), mapper);
            }
        }
        return IOUtil.readFile(Path.of(file), mapper);
    }

}
