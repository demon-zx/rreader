package ru.terentjev.util.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.List;
import java.util.Set;

public final class FileSystems {
    private static final Set<String> ignored = Set.of("jar", "jrt");
    private static final List<FileSystemProvider> providers = ServiceLoader.load(FileSystemProvider.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .filter(p -> !ignored.contains(p.getScheme()))
            .toList();

    public static FileSystem newFileSystem(Path file) throws IOException {
        for (FileSystemProvider provider : providers) {
            try {
                return provider.newFileSystem(file, Map.of());
            } catch (UnsupportedOperationException ignored) {
                // ignored
            }
        }
        return null;
    }

    public static FileSystem newFileSystem(URI file) throws IOException {
        for (FileSystemProvider provider : providers) {
            try {
                return provider.newFileSystem(file, Map.of());
            } catch (UnsupportedOperationException | IllegalArgumentException ignored) {
                // ignored
            }
        }
        throw new UnsupportedOperationException("Not support filesystem for: " + file);
    }
}
