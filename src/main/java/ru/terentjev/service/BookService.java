package ru.terentjev.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.terentjev.exception.InvalidArgumentException;
import ru.terentjev.repository.BookRepository;
import ru.terentjev.service.mapper.BookMapper;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.Example;

import java.text.MessageFormat;
import java.util.UUID;

@Service
public class BookService {
    public static final String TYPE_NAME = Example.class.getSimpleName();
    private final BookRepository repository;

    public BookService(
            BookRepository repository
    ) {
        this.repository = repository;
    }


    @Transactional
    public Book save(Book example) {
        return BookMapper.mapper.from(repository.save(BookMapper.mapper.to(example)));
    }

    public Book findById(UUID id) throws InvalidArgumentException {
        return repository.findById(id)
                .map(BookMapper.mapper::from)
                .orElseThrow(() -> new InvalidArgumentException(
                        MessageFormat.format("Not found {1} with ID: {0}", id, TYPE_NAME)
                ));
    }

}
