package ru.terentjev.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.terentjev.repository.model.BookEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends CrudRepository<BookEntity, UUID> {

    Optional<BookEntity> findByPath(String path);
}
