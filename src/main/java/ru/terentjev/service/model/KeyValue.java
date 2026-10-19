package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

public final class KeyValue {
    private final @NonNull String key;
    private final @NonNull String value;

    public KeyValue(
            @NonNull String key,
            @NonNull String value
    ) {
        this.key = key;
        this.value = value;
    }

    public @NonNull String getKey() {
        return key;
    }

    public @NonNull String getValue() {
        return value;
    }
}
