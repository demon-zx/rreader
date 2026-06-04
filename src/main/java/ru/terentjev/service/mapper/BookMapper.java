package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.service.model.Book;

@Mapper(config = Config.class, uses = PathMapper.class)
public abstract class BookMapper extends AbstractPageMapper<Book, BookEntity> {
    public static BookMapper mapper = Mappers.getMapper(BookMapper.class);

    public abstract Book from(BookEntity source);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @InheritInverseConfiguration
    public abstract BookEntity to(Book source);

    public String trim(String source) {
        if (source == null) {
            return null;
        }
        return source.trim();
    }

}