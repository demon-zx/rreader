package ru.terentjev.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.repository.BookRepository;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.service.mapper.BookMapper;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.Page;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BookService {
    public static final String TYPE_NAME = Book.class.getSimpleName();
    public static final int DEFAULT_PAGE = 0;
    public static final int MAX_PAGE_SIZE = 100;
    private final BookRepository repository;

    public BookService(
            BookRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public Book save(Book item) {
        BookEntity mapped = BookMapper.mapper.to(item);
        BookEntity entity = mapped;
        if (mapped.id() != null) {
            entity = repository.findById(mapped.id())
                    .map(
                            existing -> BookMapper.mapper.copy(
                                    mapped,
                                    existing.createdAt(),
                                    existing.createdBy()
                            )
                    )
                    .orElse(mapped);
        }
        return BookMapper.mapper.from(repository.save(entity));
    }

    public Book findById(UUID id) throws InvalidArgumentException {
        return repository.findById(id)
                .map(BookMapper.mapper::from)
                .orElseThrow(
                        () -> new InvalidArgumentException(
                                MessageFormat.format("Not found {1} with id: {0}", id, TYPE_NAME)
                        )
                );
    }

    public Optional<Book> findByUri(URI uri) {
        return repository.findByUri(uri.toString())
                .map(BookMapper.mapper::from);
    }

    /**
     * Книги по списку идентификаторов, сохраняя порядок {@code ids}.
     * Отсутствующие идентификаторы пропускаются.
     */
    public List<Book> findByIds(Collection<UUID> ids) {
        Map<UUID, BookEntity> found = repository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(BookEntity::id, e -> e));
        return ids.stream()
                .map(found::get)
                .filter(Objects::nonNull)
                .map(BookMapper.mapper::from)
                .toList();
    }

    /**
     * Книги, чей URI содержит указанный путь. Схема URI не проверяется:
     * uri обычного файла начинается с file:/, а книги внутри архивов —
     * с scheme архиватора (zip:, в будущем другие), при этом путь архива
     * входит в URI книги как подстрока.
     *
     * Вхождение считается по границе: за путём должен следовать символ '/',
     * '!' (разделитель записи внутри архива) или конец URI, поэтому архив
     * не совпадает с одноимёнными соседями (1.zip и 2.zip).
     *
     * @param path закодированный путь в формате URI (например из Path.toUri())
     */
    public List<Book> findByUriContaining(String path) {
        try (Stream<BookEntity> stream = repository.findAllAsStream()) {
            return stream
                    .map(BookMapper.mapper::from)
                    .filter(
                            book -> containsPath(
                                    book.getUri()
                                            .toString(),
                                    path
                            )
                    )
                    .toList();
        }
    }

    private static boolean containsPath(String uri, String path) {
        int index = uri.indexOf(path);
        while (index >= 0) {
            int end = index + path.length();
            if (end == uri.length() || uri.charAt(end) == '/' || uri.charAt(end) == '!') {
                return true;
            }
            index = uri.indexOf(path, index + 1);
        }
        return false;
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }

    public Page<Book> findAll(String q, int page, int pageSize) throws InvalidArgumentException {
        if (page < DEFAULT_PAGE) {
            throw new InvalidArgumentException(
                    MessageFormat.format("Номер страницы не может быть отрицательным: {0}", page)
            );
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidArgumentException(
                    MessageFormat.format(
                            "Размер страницы должен быть от 1 до {0}: {1}",
                            MAX_PAGE_SIZE,
                            pageSize
                    )
            );
        }
        Pageable p = PageRequest.of(
                page,
                pageSize,
                Sort.by("author", "title")
                        .ascending()
        );
        if (StringUtils.isBlank(q)) {
            return BookMapper.mapper.from(repository.findAll(p));
        } else {
            return BookMapper.mapper.from(repository.search(q, p));
        }
    }
}
