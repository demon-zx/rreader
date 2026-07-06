package ru.terentjev.util.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ZipFileSystemProvider extends FileSystemProvider {

    private final Map<Path, ZipFileSystem> filesystems = new ConcurrentHashMap<>();
    private final Map<Path, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public String getScheme() {
        return "zip";
    }

    protected Path getArchivePath(URI uri) {
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not: " + getScheme());
        }
        String part = uri.getRawSchemeSpecificPart();
        int index = part.indexOf("!");
        if (index != -1) {
            part = part.substring(0, index);
        }
        return Path.of(URI.create(part))
                .toAbsolutePath();
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        Path archive = getArchivePath(uri);
        return newFileSystem(archive, env);
    }

    @Override
    public FileSystem newFileSystem(Path archive, Map<String, ?> env) throws IOException {
        return locked(
                archive,
                () -> filesystems.compute(
                        archive.toRealPath(),
                        (rp, fs) -> {
                            if (fs != null) {
                                throw new FileSystemAlreadyExistsException();
                            }
                            try {
                                return new ZipFileSystem(this, archive);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                )
        );
    }

    private <T> T locked(Path archive, Callable<T> action) throws IOException {
        try {
            var lock = locks.computeIfAbsent(archive, a -> new ReentrantLock());
            if (lock.tryLock(1, TimeUnit.MINUTES)) {
                return action.call();
            }
            throw new IOException("Archive " + archive + " is locked.");
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public ZipFileSystem getFileSystem(URI uri) {
        try {
            ZipFileSystem fs = filesystems.get(getArchivePath(uri).toRealPath());
            if (fs == null) {
                throw new FileSystemNotFoundException();
            }
            return fs;
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public Path getPath(URI uri) {
        String part = uri.getSchemeSpecificPart();
        int index = part.indexOf("!");
        if (index == -1) {
            throw new IllegalArgumentException(
                    "URI: " + uri + " does not contain path info ex. zip:file:/c:/foo.zip!/BAR"
            );
        }
        return getFileSystem(uri).getPath(part.substring(index + 1));
    }

    private ZipPath castPath(Path path) {
        if (path instanceof ZipPath) {
            return (ZipPath) path;
        }
        throw new ProviderMismatchException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        return castPath(path).newByteChannel(options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        return new ZipDirectoryStream(castPath(dir), filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDirectory'");
    }

    @Override
    public void delete(Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return Objects.equals(path, path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isHidden'");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileStore'");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkAccess'");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileAttributeView'");
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(
            Path path,
            Class<A> type,
            LinkOption... options
    ) throws IOException {
        return castPath(path).readAttributes(type);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readAttributes'");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    public void close(Path archive) {
        filesystems.remove(archive);
        Lock lock = locks.remove(archive);
        if (lock != null) {
            lock.unlock();
        }
    }
}
