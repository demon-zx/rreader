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
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {
    public static final String TYPE_NAME = Book.class.getSimpleName();
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
                    .map(existing -> BookMapper.mapper.copy(
                            mapped, existing.createdAt(), existing.createdBy()))
                    .orElse(mapped);
        }
        return BookMapper.mapper.from(repository.save(entity));
    }

    public Book findById(UUID id) throws InvalidArgumentException {
        return repository.findById(id)
                .map(BookMapper.mapper::from)
                .orElseThrow(() -> new InvalidArgumentException(
                        MessageFormat.format("Not found {1} with id: {0}", id, TYPE_NAME)
                ));
    }

    public Optional<Book> findByUri(URI uri) {
        return repository.findByUri(uri.toString())
                .map(BookMapper.mapper::from);
    }

    public long count() {
        return repository.count();
    }

    public Page<Book> findAll(String q, int page, int pageSize) {
        Pageable p = PageRequest.of(page, pageSize, Sort.by("author", "title").ascending());
        if (StringUtils.isBlank(q)) {
            return BookMapper.mapper.from(repository.findAll(p));

        } else {
            return BookMapper.mapper.from(repository.search(q, p));
        }
    }

}
