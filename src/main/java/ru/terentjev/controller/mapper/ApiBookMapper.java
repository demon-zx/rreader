package ru.terentjev.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.terentjev.controller.model.ApiBook;
import ru.terentjev.controller.model.ApiInfo;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.render.Info;

@Mapper(config = Config.class)
public abstract class ApiBookMapper extends ApiPageMapper<ApiBook, Book> {
    public static ApiBookMapper mapper = Mappers.getMapper(ApiBookMapper.class);

    @Mapping(target = "isbn", source = "isbn", qualifiedByName = "isbn")
    @Mapping(target = "size", source = "size", qualifiedByName = "size")
    public abstract ApiBook from(Book source);

    @Named("isbn")
    public String isbn(Long source) {
        if (source == null) {
            return "N/A";
        }
        return String.valueOf(source);
    }

    @Named("size")
    public String size(long source) {
        return String.valueOf(source);
    }
}