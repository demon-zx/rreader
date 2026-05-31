package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
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
    @NonNull
    private final Instant createdAt;
    private final String createdBy;
    @NonNull
    private final Instant updatedAt;
    private final String updatedBy;

    public Book(
            UUID id,
            @NonNull String title,
            String author,
            String language,
            @NonNull String type,
            @NonNull String path,
            @NonNull String checksum,
            Long isbn,
            @NonNull Instant createdAt,
            String createdBy,
            @NonNull Instant updatedAt,
            String updatedBy
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.language = language;
        this.type = type;
        this.path = path;
        this.checksum = checksum;
        this.isbn = isbn;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
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

    public long getIsbn() {
        return isbn;
    }

    public @NonNull Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public @NonNull Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
