package ru.terentjev.util.fs;

import org.unbescape.uri.UriEscape;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZipPath implements Path {

    private final ZipFileSystem fileSystem;
    private final String path;
    private final boolean absolute;

    public ZipPath(ZipFileSystem fileSystem, String path) {
        this.fileSystem = fileSystem;
        this.path = path;
        absolute = true;
    }

    public ZipPath(ZipFileSystem fileSystem, String path, boolean absolute) {
        this.fileSystem = fileSystem;
        this.path = path;
        this.absolute = absolute;
    }

    @Override
    public ZipFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public Path getRoot() {
        return new ZipPath(fileSystem, "/");
    }

    @Override
    public Path getFileName() {
        String[] split = path.split("/");
        return new ZipPath(fileSystem, split[split.length - 1], false);
    }

    @Override
    public Path getParent() {
        if (path.equals("/")) {
            return null;
        }
        String[] split = path.split("/");
        if (split.length == 1) {
            return getRoot();
        }
        String parent = Stream.of(split)
                .limit(split.length - 1)
                .collect(Collectors.joining("/")) + "/";
        return new ZipPath(fileSystem, parent, true);
    }

    @Override
    public int getNameCount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNameCount'");
    }

    @Override
    public Path getName(int index) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'subpath'");
    }

    @Override
    public boolean startsWith(Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startsWith'");
    }

    @Override
    public boolean endsWith(Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'endsWith'");
    }

    @Override
    public Path normalize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'normalize'");
    }

    @Override
    public Path resolve(Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }

    @Override
    public Path relativize(Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'relativize'");
    }

    @Override
    public URI toUri() {
        ZipFileSystemProvider provider = fileSystem.provider();
        Path archive = fileSystem.getArchive();
//        String entry = URLEncoder.encode(path, StandardCharsets.UTF_8);
        String entry = path.replace(" ", "%20");
        return URI.create(provider.getScheme() + ":" + archive.toUri() + "!/" + entry);
    }

    @Override
    public Path toAbsolutePath() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toAbsolutePath'");
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toRealPath'");
    }

    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'register'");
    }

    @Override
    public int compareTo(Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'compareTo'");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZipPath paths)) {
            return false;
        }
        return Objects.equals(path, paths.path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

    @Override
    public String toString() {
        return path;
    }

    public <A extends BasicFileAttributes> A readAttributes(Class<A> type) throws IOException {
        if (type == BasicFileAttributes.class) {
            //noinspection unchecked
            return (A) fileSystem.readAttributes(this);
        }
        throw new UnsupportedOperationException("Not supported attributes of type: " + type);
    }

    public SeekableByteChannel newByteChannel(
            Set<? extends OpenOption> options,
            FileAttribute<?>[] attrs
    ) throws IOException {
        return fileSystem.newByteChannel(this, options, attrs);
    }
}
