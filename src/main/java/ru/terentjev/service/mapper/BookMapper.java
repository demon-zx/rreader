package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.service.model.Book;

import java.time.Instant;

@Mapper(config = Config.class, uses = UriMapper.class)
public abstract class BookMapper extends AbstractPageMapper<Book, BookEntity> {
    public static BookMapper mapper = Mappers.getMapper(BookMapper.class);

    public abstract Book from(BookEntity source);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @InheritInverseConfiguration
    public abstract BookEntity to(Book source);

    /**
     * Возвращает копию {@code source} с датой/автором создания, взятыми из ранее
     * сохранённой записи, — чтобы аудит создания не затирался при обновлении.
     */
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    public abstract BookEntity copy(BookEntity source, Instant createdAt, String createdBy);
}