package ru.terentjev.repository.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("user")
public record UserEntity(
        @Id @Column("id") UUID id,
        @Column("username") String username,
        @Column("password") String password,
        @Column("admin") boolean admin,
        @Column("enabled") boolean enabled,
        @Column("compromised") boolean compromised
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
