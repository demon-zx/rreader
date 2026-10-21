package ru.terentjev.controller.mapper;

import ru.terentjev.controller.model.ApiPage;
import ru.terentjev.service.model.Page;

import java.util.List;

public abstract class ApiPageMapper<T, E> {
    public ApiPage<T> from(Page<E> source) {
        List<T> converted = source.getItems()
                .stream()
                .map(this::from)
                .toList();
        return new ApiPage<>(converted, source.getAll(), source.getAllPages());
    }

    public abstract T from(E source);
}