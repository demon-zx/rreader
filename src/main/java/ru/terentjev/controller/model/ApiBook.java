package ru.terentjev.controller.model;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class ApiBook {

    private final UUID id;
    @NonNull
    private final String title;
    private final String author;
    private final String sequenceName;
    private final Integer sequenceNumber;
    @NonNull
    private final String type;
    private final String isbn;
    @NonNull
    private final String size;

    public ApiBook(
            UUID id,
            @NonNull String title,
            String author,
            String sequenceName,
            Integer sequenceNumber,
            @NonNull String type,
            String isbn,
            @NonNull String size
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.sequenceName = sequenceName;
        this.sequenceNumber = sequenceNumber;
        this.type = type;
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

    public String getSequenceName() {
        return sequenceName;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public @NonNull String getType() {
        return type;
    }

    public String getIsbn() {
        return isbn;
    }

    public @NonNull String getSize() {
        return size;
    }
}
