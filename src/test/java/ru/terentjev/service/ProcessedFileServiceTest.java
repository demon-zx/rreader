package ru.terentjev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.terentjev.repository.ProcessedFileRepository;
import ru.terentjev.repository.model.ProcessedFileEntity;
import ru.terentjev.service.model.ProcessResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedFileServiceTest {

    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @TempDir
    Path tempDir;

    @Mock
    private ProcessedFileRepository repository;

    private ProcessedFileService service;
    private Path file;

    @BeforeEach
    void setUp() throws IOException {
        Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));
        service = new ProcessedFileService(repository, clock);
        file = Files.createFile(tempDir.resolve("book.fb2"));
    }

    @Test
    void isChangedWhenNoRecord() throws IOException {
        when(repository.findByPath(file.toAbsolutePath().toString())).thenReturn(Optional.empty());

        assertTrue(service.isChanged(file));
    }

    @Test
    void isNotChangedWhenSizeAndModifiedAtTheSame() throws IOException {
        ProcessedFileEntity entity = entity(true, Files.size(file), modifiedAt());
        when(repository.findByPath(file.toAbsolutePath().toString())).thenReturn(Optional.of(entity));

        assertFalse(service.isChanged(file));
    }

    @Test
    void isChangedWhenSizeDiffers() throws IOException {
        ProcessedFileEntity entity = entity(true, Files.size(file) + 1, modifiedAt());
        when(repository.findByPath(file.toAbsolutePath().toString())).thenReturn(Optional.of(entity));

        assertTrue(service.isChanged(file));
    }

    @Test
    void isChangedWhenModifiedAtDiffers() throws IOException {
        ProcessedFileEntity entity = entity(true, Files.size(file), modifiedAt().plusSeconds(1));
        when(repository.findByPath(file.toAbsolutePath().toString())).thenReturn(Optional.of(entity));

        assertTrue(service.isChanged(file));
    }

    @Test
    void isChangedWhenNotProcessed() throws IOException {
        ProcessedFileEntity entity = entity(false, Files.size(file), modifiedAt());
        when(repository.findByPath(file.toAbsolutePath().toString())).thenReturn(Optional.of(entity));

        assertTrue(service.isChanged(file));
    }

    @Test
    void markProcessedStoresSizeAndModifiedAt() throws IOException {
        when(repository.findByPath(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        long size = Files.size(file);
        Instant modifiedAt = modifiedAt();

        service.markProcessed(file, ProcessResult.SUCCESS, null, size, modifiedAt);

        ArgumentCaptor<ProcessedFileEntity> captor = ArgumentCaptor.forClass(ProcessedFileEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(size, captor.getValue().size());
        assertEquals(modifiedAt, captor.getValue().modifiedAt());
        assertTrue(captor.getValue().processed());
    }

    private ProcessedFileEntity entity(boolean processed, Long size, Instant modifiedAt) {
        return new ProcessedFileEntity(
                null,
                file.toAbsolutePath().toString(),
                processed,
                ProcessResult.SUCCESS.name(),
                null,
                NOW,
                size,
                modifiedAt,
                null,
                null,
                null
        );
    }

    private Instant modifiedAt() throws IOException {
        return Files.getLastModifiedTime(file).toInstant();
    }
}
