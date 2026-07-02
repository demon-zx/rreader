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

    public ProcessedFile(
            UUID id,
            @NonNull Path path,
            boolean processed,
            ProcessResult result,
            String error,
            Instant processedAt
    ) {
        this.id = id;
        this.path = path;
        this.processed = processed;
        this.result = result;
        this.error = error;
        this.processedAt = processedAt;
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
}
