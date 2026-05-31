package ru.terentjev.repository.model;

import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.Id;
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
        @NonNull
        @Column("created_at")
        Instant createdAt,
        @Column("created_by")
        String createdBy,
        @NonNull
        @Column("updated_at")
        Instant updatedAt,
        @Column("updated_by")
        String updatedBy
) {


}
