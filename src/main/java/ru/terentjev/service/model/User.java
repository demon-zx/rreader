package ru.terentjev.service.model;

import java.util.UUID;

public final class User {

    private final UUID id;
    private final String username;
    private final String password;
    private final boolean admin;
    private final boolean enabled;
    private final boolean compromised;

    public User(
            UUID id,
            String username,
            String password,
            boolean admin,
            boolean enabled,
            boolean compromised
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.admin = admin;
        this.enabled = enabled;
        this.compromised = compromised;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCompromised() {
        return compromised;
    }
}
