package ru.terentjev.service;

import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.repository.BookmarkRepository;
import ru.terentjev.repository.model.BookmarkEntity;
import ru.terentjev.service.mapper.BookmarkMapper;
import ru.terentjev.service.model.Bookmark;

import java.text.MessageFormat;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BookmarkService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BookmarkService.class);
    public static final String TYPE_NAME = Bookmark.class.getSimpleName();
    public static final String CURRENT = "$current$";
    private final BookmarkRepository repository;
    private final Lock insertLock = new ReentrantLock();

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
                .orElseThrow(
                        () -> new InvalidArgumentException(
                                MessageFormat.format("Not found {1} with id: {0}", id, TYPE_NAME)
                        )
                );
    }

    public long findCurrentPosition(String username, UUID bookId) throws InvalidArgumentException {
        return repository.findByUsernameAndTitleAndBookId(username, CURRENT, bookId)
                .map(BookmarkEntity::position)
                .orElse(1L);
    }

    /**
     * «Текущие» закладки, отсортированные по дате изменения закладки по
     * убыванию (не более {@code limit} записей).
     */
    public List<Bookmark> findRecent(String username, int limit) {
        return repository.findCurrent(
                username,
                CURRENT,
                PageRequest.of(0, limit, Sort.by("updatedAt").descending())
        )
                .stream()
                .map(BookmarkMapper.mapper::from)
                .toList();
    }

    @Transactional
    public void deleteByBook(UUID bookId) {
        repository.deleteByBookId(bookId);
    }

    @Transactional
    public void saveCurrentPosition(String username, UUID bookId, long position) {
        Optional<BookmarkEntity> exist = repository.findByUsernameAndTitleAndBookId(username, CURRENT, bookId);
        if (exist.isEmpty()) {
            try {
                if (insertLock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        exist = repository.findByUsernameAndTitleAndBookId(username, CURRENT, bookId);
                        if (exist.isEmpty()) {
                            repository.save(new BookmarkEntity(null, username, CURRENT, bookId, position, null, null));
                        }
                    } finally {
                        insertLock.unlock();
                    }
                }
            } catch (InterruptedException ie) {
                throw new IllegalStateException(ie);
            }
        }
        exist.map(b -> update(b, position))
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
