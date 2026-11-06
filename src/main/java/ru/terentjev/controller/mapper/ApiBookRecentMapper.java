package ru.terentjev.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import ru.terentjev.controller.model.ApiBookRecent;
import ru.terentjev.service.model.BookRecent;

@Mapper(config = Config.class)
public abstract class ApiBookRecentMapper {
    public static ApiBookRecentMapper mapper = Mappers.getMapper(ApiBookRecentMapper.class);

    @Mapping(target = "isbn", source = "isbn", qualifiedByName = "isbn")
    @Mapping(target = "size", source = "size", qualifiedByName = "size")
    public abstract ApiBookRecent from(BookRecent source);

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
