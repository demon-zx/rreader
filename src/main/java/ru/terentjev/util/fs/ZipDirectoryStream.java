package ru.terentjev.util.fs;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ZipDirectoryStream implements DirectoryStream<Path> {

    private Iterator<ZipPath> files;

    public ZipDirectoryStream(ZipPath path, DirectoryStream.Filter<? super java.nio.file.Path> filter) {
        ZipFileSystem fileSystem = path.getFileSystem();
        files = fileSystem.files(path)
                .stream()
                .filter(f -> {
                    try {
                        return filter.accept(f);
                    } catch (IOException e) {
                        throw new DirectoryIteratorException(e);
                    }
                })
                .toList()
                .iterator();
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return files != null && files.hasNext();
            }

            @Override
            public Path next() {
                if (files == null) {
                    throw new NoSuchElementException();
                }
                return files.next();
            }
        };
    }

    @Override
    public void close() {
        files = null;
    }
}
