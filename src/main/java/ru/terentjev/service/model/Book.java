package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.UUID;

public final class Book {
    private final UUID id;
    @NonNull
    private final String title;
    private final String author;
    private final String language;
    private final String sequenceName;
    private final Integer sequenceNumber;
    @NonNull
    private final String type;
    @NonNull
    private final URI uri;
    @NonNull
    private final String checksum;
    private final Long isbn;
    private final long size;

    public Book(
            UUID id,
            @NonNull String title,
            String author,
            String language,
            String sequenceName,
            Integer sequenceNumber,
            @NonNull String type,
            @NonNull URI uri,
            @NonNull String checksum,
            Long isbn,
            long size
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.language = language;
        this.sequenceName = sequenceName;
        this.sequenceNumber = sequenceNumber;
        this.type = type;
        this.uri = uri;
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

    public String getSequenceName() {
        return sequenceName;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public @NonNull String getType() {
        return type;
    }

    public @NonNull URI getUri() {
        return uri;
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
