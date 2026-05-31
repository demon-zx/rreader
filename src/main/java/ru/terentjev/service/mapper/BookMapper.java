package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.service.model.Book;

@Mapper(config = Config.class)
public abstract class BookMapper {
    public static BookMapper mapper = Mappers.getMapper(BookMapper.class);

    public abstract Book from(BookEntity source);

    @InheritInverseConfiguration
    public abstract BookEntity to(Book source);

}