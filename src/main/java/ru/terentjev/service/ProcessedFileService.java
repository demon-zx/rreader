package ru.terentjev.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.repository.ProcessedFileRepository;
import ru.terentjev.repository.model.ProcessedFileEntity;
import ru.terentjev.service.mapper.ProcessedFileMapper;
import ru.terentjev.service.model.ProcessResult;
import ru.terentjev.service.model.ProcessedFile;

import java.nio.file.Path;
import java.time.Clock;
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
     * Записывает результат обработки файла. Если запись по пути уже существует,
     * обновляет её (сохраняя идентификатор и дату создания), иначе создаёт новую.
     */
    @Transactional
    public ProcessedFile markProcessed(Path path, ProcessResult result, String error) {
        String value = path.toString();
        ProcessedFileEntity entity = repository.findByPath(value)
                .map(existing -> update(existing, result, error))
                .orElseGet(() -> create(value, result, error));
        return ProcessedFileMapper.mapper.from(repository.save(entity));
    }

    private ProcessedFileEntity create(String path, ProcessResult result, String error) {
        return new ProcessedFileEntity(
                null,
                path,
                true,
                result.name(),
                error,
                clock.instant(),
                null,
                null,
                null
        );
    }

    private ProcessedFileEntity update(ProcessedFileEntity source, ProcessResult result, String error) {
        return new ProcessedFileEntity(
                source.id(),
                source.path(),
                true,
                result.name(),
                error,
                clock.instant(),
                source.createdAt(),
                null,
                null
        );
    }
}
