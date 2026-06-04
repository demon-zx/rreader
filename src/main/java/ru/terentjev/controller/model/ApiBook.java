package ru.terentjev.controller.model;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class ApiBook {

    private final UUID id;
    @NonNull
    private final String title;
    private final String author;
    @NonNull
    private final String type;
    private final String isbn;
    @NonNull
    private final String size;

    public ApiBook(UUID id, @NonNull String title, String author, @NonNull String type, String isbn, String size) {
        this.id = id;
        this.title = title;
        this.author = author;
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
