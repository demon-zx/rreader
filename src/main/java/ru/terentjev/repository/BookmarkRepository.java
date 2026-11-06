package ru.terentjev.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.terentjev.repository.model.BookmarkEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository
        extends
        ListCrudRepository<BookmarkEntity, UUID>,
        ListPagingAndSortingRepository<BookmarkEntity, UUID>,
        BookmarkRepositoryCurrent {
    Optional<BookmarkEntity> findByUsernameAndTitleAndBookId(String username, String title, UUID bookId);

    /**
     * Удаление закладок книги. Используется явный {@code @Modifying @Query}, а не
     * derived-метод {@code deleteByBookId}: в Spring Data JDBC derived-удаление
     * с void-результатом молча не выполняет никакого SQL.
     */
    @Modifying
    @Query("delete from bookmark where book_id = :bookId")
    void deleteByBookId(@Param("bookId") UUID bookId);
}
