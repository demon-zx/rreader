package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public final class Book {

    private final UUID id;
    @NonNull
    private final String title;
    private final String author;
    private final String language;
    @NonNull
    private final String type;
    @NonNull
    private final String path;
    @NonNull
    private final String checksum;
    private final Long isbn;
    private final long size;

    public Book(
            UUID id,
            @NonNull String title,
            String author,
            String language,
            @NonNull String type,
            @NonNull String path,
            @NonNull String checksum,
            Long isbn,
            long size
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.language = language;
        this.type = type;
        this.path = path;
        this.checksum = checksum;
        this.isbn = isbn;
        this.size = size;
    }

    public UUID getId() {
        return id;
    }

    public @NonNull String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getLanguage() {
        return language;
    }

    public @NonNull String getType() {
        return type;
    }

    public @NonNull String getPath() {
        return path;
    }

    public @NonNull String getChecksum() {
        return checksum;
    }

    public Long getIsbn() {
        return isbn;
    }

    public long getSize() {
        return size;
    }
}
