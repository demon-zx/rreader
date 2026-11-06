package ru.terentjev.repository;

import org.springframework.data.domain.Pageable;
import ru.terentjev.repository.model.BookmarkEntity;

import java.util.List;

public interface BookmarkRepositoryCurrent {
    List<BookmarkEntity> findCurrent(String username, String title, Pageable pageable);
}
