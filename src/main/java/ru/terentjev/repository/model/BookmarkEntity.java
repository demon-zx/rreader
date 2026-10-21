package ru.terentjev.repository.model;

import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("bookmark")
public record BookmarkEntity(
        @Id @Column("id") UUID id,
        @NonNull @Column("username") String username,
        @NonNull @Column("title") String title,
        @NonNull @Column("book_id") UUID bookId,
        @Column("position") long position,
        @CreatedDate @Column("created_at") Instant createdAt,
        @LastModifiedDate @Column("updated_at") Instant updatedAt
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
