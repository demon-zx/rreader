package ru.terentjev.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import ru.terentjev.repository.model.BookEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository
        extends ListCrudRepository<BookEntity, UUID>, ListPagingAndSortingRepository<BookEntity, UUID> {

    Optional<BookEntity> findByPath(String path);

    Page<BookEntity> findAllByAuthorContainingIgnoreCaseOrTitleContainingIgnoreCase(
            String author,
            String title,
            Pageable pageable
    );

}
