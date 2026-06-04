package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public final class Bookmark {
    private final UUID id;
    private final @NonNull String title;
    private final @NonNull String username;
    private final @NonNull UUID bookId;
    private final long position;

    public Bookmark(
            UUID id,
            @NonNull
            String username,
            @NonNull
            String title,
            @NonNull
            UUID bookId,
            long position
    ) {
        this.id = id;
        this.title = title;
        this.username = username;
        this.bookId = bookId;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public @NonNull String getTitle() {
        return title;
    }

    public @NonNull String getUsername() {
        return username;
    }

    public @NonNull UUID getBookId() {
        return bookId;
    }

    public long getPosition() {
        return position;
    }
}
