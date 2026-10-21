package ru.terentjev.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import ru.terentjev.repository.model.BookEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface BookRepository
        extends
        ListCrudRepository<BookEntity, UUID>,
        ListPagingAndSortingRepository<BookEntity, UUID>,
        BookRepositorySearch {
    Optional<BookEntity> findByUri(String uri);

    @Query("select * from book")
    Stream<BookEntity> findAllAsStream();
}
