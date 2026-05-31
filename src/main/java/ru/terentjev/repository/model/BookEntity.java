package ru.terentjev.repository.model;

import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("book")
public record BookEntity(
        @Id
        @Column("id")
        UUID id,
        @NonNull
        @Column("title")
        String title,
        @Column("author")
        String author,
        @Column("language")
        String language,
        @NonNull
        @Column("type")
        String type,
        @NonNull
        @Column("path")
        String path,
        @NonNull
        @Column("checksum")
        String checksum,
        @Column("isbn")
        Long isbn,
        @CreatedDate
        @Column("created_at")
        Instant createdAt,
        @CreatedBy
        @Column("created_by")
        String createdBy,
        @LastModifiedDate
        @Column("updated_at")
        Instant updatedAt,
        @LastModifiedBy
        @Column("updated_by")
        String updatedBy
) implements Persistable<UUID> {


    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
