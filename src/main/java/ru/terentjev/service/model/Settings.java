package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

import java.util.List;

public final class Settings {
    private final @NonNull String scope;
    private final @NonNull List<KeyValue> settings;

    public Settings(
            @NonNull String scope,
            @NonNull List<KeyValue> settings
    ) {
        this.scope = scope;
        this.settings = settings;
    }

    public @NonNull String getScope() {
        return scope;
    }

    public @NonNull List<KeyValue> getSettings() {
        return settings;
    }
}
