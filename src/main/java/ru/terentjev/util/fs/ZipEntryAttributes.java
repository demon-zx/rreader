package ru.terentjev.util.fs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class ZipEntryAttributes implements BasicFileAttributes {

    private final FileTime lastModifiedTime;
    private final FileTime lastAccessTime;
    private final FileTime creationTime;
    private final boolean file;
    private final long size;

    public ZipEntryAttributes(
            FileTime lastModifiedTime,
            FileTime lastAccessTime,
            FileTime creationTime,
            boolean file,
            long size
    ) {
        this.lastModifiedTime = lastModifiedTime;
        this.lastAccessTime = lastAccessTime;
        this.creationTime = creationTime;
        this.file = file;
        this.size = size;
    }

    @Override
    public FileTime lastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public FileTime lastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public FileTime creationTime() {
        return creationTime;
    }

    @Override
    public boolean isRegularFile() {
        return file;
    }

    @Override
    public boolean isDirectory() {
        return !file;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object fileKey() {
        return null;
    }
}
