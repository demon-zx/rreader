package ru.terentjev.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.terentjev.repository.model.BookEntity;

public interface BookRepositorySearch {

    Page<BookEntity> search(String q, Pageable pageable);

}
