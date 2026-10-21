package ru.terentjev.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import ru.terentjev.repository.model.ProcessedFileEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedFileRepository
        extends
        ListCrudRepository<ProcessedFileEntity, UUID>,
        ListPagingAndSortingRepository<ProcessedFileEntity, UUID> {
    Optional<ProcessedFileEntity> findByPath(String path);
}
