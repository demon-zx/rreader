package ru.terentjev.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import ru.terentjev.repository.model.BookmarkEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository
        extends ListCrudRepository<BookmarkEntity, UUID>, ListPagingAndSortingRepository<BookmarkEntity, UUID> {

    Optional<BookmarkEntity> findByUsernameAndTitleAndBookId(String username, String title, UUID bookId);

}
