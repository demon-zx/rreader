package ru.terentjev.controller.model;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class ApiBookRecent extends ApiBook {
    private final long position;
    private final int pages;
    private final int percent;

    public ApiBookRecent(
            UUID id,
            @NonNull String title,
            String author,
            String sequenceName,
            Integer sequenceNumber,
            @NonNull String type,
            String isbn,
            @NonNull String size,
            long position,
            int pages,
            int percent
    ) {
        super(id, title, author, sequenceName, sequenceNumber, type, isbn, size);
        this.position = position;
        this.pages = pages;
        this.percent = percent;
    }

    public long getPosition() {
        return position;
    }

    public int getPages() {
        return pages;
    }

    public int getPercent() {
        return percent;
    }
}
