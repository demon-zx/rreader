package ru.terentjev.service.mapper;

import ru.terentjev.service.model.Page;

import java.util.List;

public abstract class AbstractPageMapper<T, E> {
    public Page<T> from(org.springframework.data.domain.Page<E> source) {
        List<T> converted = source.stream()
                .map(this::from)
                .toList();
        return new Page<>(
                converted,
                (int) source.getTotalElements(),
                source.getTotalPages()
        );
    }

    public abstract T from(E source);
}