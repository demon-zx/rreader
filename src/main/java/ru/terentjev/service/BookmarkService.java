package ru.terentjev.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.repository.BookmarkRepository;
import ru.terentjev.repository.model.BookmarkEntity;
import ru.terentjev.service.mapper.BookmarkMapper;
import ru.terentjev.service.model.Bookmark;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookmarkService {
    public static final String TYPE_NAME = Bookmark.class.getSimpleName();
    public static final String CURRENT = "$current$";
    private final BookmarkRepository repository;

    public BookmarkService(
            BookmarkRepository repository
    ) {
        this.repository = repository;
    }


    @Transactional
    public Bookmark save(Bookmark item) {
        return BookmarkMapper.mapper.from(repository.save(BookmarkMapper.mapper.to(item)));
    }

    public Bookmark findById(UUID id) throws InvalidArgumentException {
        return repository.findById(id)
                .map(BookmarkMapper.mapper::from)
                .orElseThrow(() -> new InvalidArgumentException(
                        MessageFormat.format("Not found {1} with id: {0}", id, TYPE_NAME)
                ));
    }

    public long findCurrentPosition(String username, UUID bookId) throws InvalidArgumentException {
        return repository.findByUsernameAndTitleAndBookId(username, CURRENT, bookId)
                .map(BookmarkEntity::position)
                .orElse(0L);
    }

    @Transactional
    public void saveCurrentPosition(String username, UUID bookId, long position) {
        repository.findByUsernameAndTitleAndBookId(username, CURRENT, bookId)
                .map(b -> update(b, position))
                .or(() -> Optional.of(new BookmarkEntity(null, username, CURRENT, bookId, position, null, null)))
                .ifPresent(repository::save);
    }

    private static BookmarkEntity update(BookmarkEntity source, long position) {
        return new BookmarkEntity(
                source.id(),
                source.username(),
                source.title(),
                source.bookId(),
                position,
                source.createdAt(),
                null
        );
    }
}
