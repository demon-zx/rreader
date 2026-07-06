package ru.terentjev.util.fs;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZipFileSystem extends java.nio.file.FileSystem {

    private final ZipFileSystemProvider provider;
    private final Path archive;
    private final ZipFile zip;

    public ZipFileSystem(ZipFileSystemProvider provider, Path archive) throws IOException {
        this.provider = provider;
        this.archive = archive;
        zip = new ZipFile.Builder()
                .setPath(archive)
                .get();
    }

    @Override
    public ZipFileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        try {
            if (zip != null) {
                zip.close();
            }
        } finally {
            provider().close(archive);
        }
    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isOpen'");
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isReadOnly'");
    }

    @Override
    public String getSeparator() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSeparator'");
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRootDirectories'");
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileStores'");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supportedFileAttributeViews'");
    }

    @Override
    public Path getPath(String first, String... more) {
        if (more.length == 0) {
            return new ZipPath(this, first);
        }
        return new ZipPath(this, String.join("/", more));

    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPathMatcher'");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserPrincipalLookupService'");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newWatchService'");
    }

    public ZipEntryAttributes readAttributes(ZipPath path) throws IOException {
        //TODO add unix attributes
        ZipArchiveEntry entry = zip.getEntry(path.getPath());
        if (entry == null && "/".equals(path.getPath())) {
            BasicFileAttributes rootAttrs = Files.readAttributes(
                    archive,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
            return new ZipEntryAttributes(
                rootAttrs.lastModifiedTime(),
                rootAttrs.lastAccessTime(),
                rootAttrs.creationTime(),
                false,
                0
            );
        }
        return new ZipEntryAttributes(
                entry.getLastModifiedTime(),
                entry.getLastAccessTime(),
                entry.getCreationTime(),
                !entry.isDirectory() && !entry.isUnixSymlink(),
                entry.getSize()
        );
    }

    public Collection<ZipPath> files(ZipPath path) {
        List<ZipPath> result = new ArrayList<>();
        Iterator<ZipArchiveEntry> iterator = zip.getEntries()
                .asIterator();
        String filter = path.getPath();
        String[] filterSplit = filter.split("/");
        int deep = filterSplit.length + 1;
        filter = Stream.of(filterSplit)
                .map(s -> s + "/")
                .collect(Collectors.joining());
        while (iterator.hasNext()) {
            ZipArchiveEntry e = iterator.next();
            String name = e.getName();
            String[] split = name.split("/");
            if (name.startsWith(filter) && split.length == deep) {
                result.add(new ZipPath(this, name));
            }
        }
        return result;
    }

    public SeekableByteChannel newByteChannel(
            ZipPath path,
            Set<? extends OpenOption> options,
            FileAttribute<?>[] attrs
    ) throws IOException {
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
            throw new UnsupportedOperationException("Unimplemented method 'newByteChannel' for write");
        } else {
            ZipArchiveEntry entry = zip.getEntry(path.getPath());
            if (entry != null) {
                //TODO попробовать написать свою реализацию без чтения всего архива в память
                return new SeekableInMemoryByteChannel(zip.getInputStream(entry).readAllBytes());
            } else {
                throw new IllegalArgumentException("Unknown archive entry: " + path.getPath());
            }
        }
    }

    public Path getArchive() {
        return archive;
    }
}
