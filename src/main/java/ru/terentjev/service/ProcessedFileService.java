package ru.terentjev.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.repository.ProcessedFileRepository;
import ru.terentjev.repository.model.ProcessedFileEntity;
import ru.terentjev.service.mapper.ProcessedFileMapper;
import ru.terentjev.service.model.ProcessResult;
import ru.terentjev.service.model.ProcessedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProcessedFileService {
    private final ProcessedFileRepository repository;
    private final Clock clock;

    public ProcessedFileService(
            ProcessedFileRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    public Optional<ProcessedFile> findByPath(Path path) {
        return repository.findByPath(path.toString())
                .map(ProcessedFileMapper.mapper::from);
    }

    /**
     * Файл считается изменённым, если он ещё не отмечен обработанным
     * либо изменились размер или время модификации с прошлой обработки.
     */
    public boolean isChanged(Path file) throws IOException {
        Optional<ProcessedFile> existing = findByPath(file);
        if (existing.isEmpty()) {
            return true;
        }
        ProcessedFile processedFile = existing.get();
        long size = Files.size(file);
        Instant modified = modifiedAt(file);
        return !processedFile.isProcessed() ||
                processedFile.getSize() == null || processedFile.getSize() != size ||
                processedFile.getModifiedAt() == null || !Objects.equals(processedFile.getModifiedAt(), modified);
    }

    /**
     * Записывает результат обработки файла. Если запись по пути уже существует,
     * обновляет её (сохраняя идентификатор и дату создания), иначе создаёт новую.
     */
    @Transactional
    public ProcessedFile markProcessed(Path path, ProcessResult result, String error, Long size, Instant modifiedAt) {
        String value = path.toString();
        ProcessedFileEntity entity = repository.findByPath(value)
                .map(existing -> update(existing, result, error, size, modifiedAt))
                .orElseGet(() -> create(value, result, error, size, modifiedAt));
        return ProcessedFileMapper.mapper.from(repository.save(entity));
    }

    private ProcessedFileEntity create(String path, ProcessResult result, String error, Long size, Instant modifiedAt) {
        return new ProcessedFileEntity(
                null,
                path,
                true,
                result.name(),
                error,
                clock.instant(),
                size,
                modifiedAt,
                null,
                null,
                null
        );
    }

    private ProcessedFileEntity update(
            ProcessedFileEntity source,
            ProcessResult result,
            String error,
            Long size,
            Instant modifiedAt
    ) {
        return new ProcessedFileEntity(
                source.id(),
                source.path(),
                true,
                result.name(),
                error,
                clock.instant(),
                size,
                modifiedAt,
                source.createdAt(),
                null,
                null
        );
    }

    private static Instant modifiedAt(Path file) throws IOException {
        return Files.getLastModifiedTime(file).toInstant();
    }
}
