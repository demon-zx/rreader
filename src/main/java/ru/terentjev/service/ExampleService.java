package ru.terentjev.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.terentjev.service.mapper.ExampleMapper;
import ru.terentjev.service.model.Example;
import ru.terentjev.repository.ExampleRepository;
import ru.terentjev.repository.model.ExampleEntity;
import ru.terentjev.exception.InvalidArgumentException;

import java.text.MessageFormat;

@Service
public class ExampleService {
    public static final String TYPE_NAME = Example.class.getSimpleName();
    private final ExampleRepository repository;

    public ExampleService(
            ExampleRepository repository
    ) {
        this.repository = repository;
    }


    @Transactional
    public Example save(Example example) {
        return ExampleMapper.mapper.from(repository.save(ExampleMapper.mapper.to(example)));
    }

    public Example findById(int id) throws InvalidArgumentException {
        return repository.findById(id)
                .map(ExampleMapper.mapper::from)
                .orElseThrow(() -> new InvalidArgumentException(
                    MessageFormat.format("Не найден {1} с ИД: {0}", id, TYPE_NAME)
                ));
    }

}
