package ru.terentjev.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import ru.terentjev.repository.model.BookEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository
        extends ListCrudRepository<BookEntity, UUID>,
        ListPagingAndSortingRepository<BookEntity, UUID>,
        BookRepositorySearch {

    Optional<BookEntity> findByUri(String uri);

}
