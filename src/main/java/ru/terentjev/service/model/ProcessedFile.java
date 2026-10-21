package ru.terentjev.service.model;

import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public final class ProcessedFile {

    private final UUID id;
    @NonNull
    private final Path path;
    private final boolean processed;
    private final ProcessResult result;
    private final String error;
    private final Instant processedAt;
    private final Long size;
    private final Instant modifiedAt;

    public ProcessedFile(
            UUID id,
            @NonNull Path path,
            boolean processed,
            ProcessResult result,
            String error,
            Instant processedAt,
            Long size,
            Instant modifiedAt
    ) {
        this.id = id;
        this.path = path;
        this.processed = processed;
        this.result = result;
        this.error = error;
        this.processedAt = processedAt;
        this.size = size;
        this.modifiedAt = modifiedAt;
    }

    public UUID getId() {
        return id;
    }

    public @NonNull Path getPath() {
        return path;
    }

    public boolean isProcessed() {
        return processed;
    }

    public ProcessResult getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Long getSize() {
        return size;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }
}
