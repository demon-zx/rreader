package ru.terentjev.repository.model;

import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("processed_file")
public record ProcessedFileEntity(
        @Id @Column("id") UUID id,
        @NonNull @Column("path") String path,
        @Column("processed") boolean processed,
        @Column("result") String result,
        @Column("error") String error,
        @Column("processed_at") Instant processedAt,
        @Column("size") Long size,
        @Column("modified_at") Instant modifiedAt,
        @CreatedDate @Column("created_at") Instant createdAt,
        @LastModifiedDate @Column("updated_at") Instant updatedAt,
        @LastModifiedBy @Column("updated_by") String updatedBy
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
