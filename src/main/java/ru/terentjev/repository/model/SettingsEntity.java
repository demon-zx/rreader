package ru.terentjev.repository.model;

import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("settings")
public record SettingsEntity(
        @Id
        @Column("id")
        UUID id,
        @NonNull
        @Column("device_uuid")
        UUID deviceUuid,
        @NonNull
        @Column("scope")
        String scope,
        @NonNull
        @Column("setting_key")
        String key,
        @NonNull
        @Column("setting_value")
        String value,
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
