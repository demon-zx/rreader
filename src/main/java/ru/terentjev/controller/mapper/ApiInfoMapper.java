package ru.terentjev.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.terentjev.controller.model.ApiContent;
import ru.terentjev.controller.model.ApiInfo;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.render.Content;
import ru.terentjev.service.render.OpenInfo;

@Mapper(config = Config.class)
public abstract class ApiInfoMapper {
    public static ApiInfoMapper mapper = Mappers.getMapper(ApiInfoMapper.class);

    @Mapping(target = "author", source = "sourceBook.author")
    @Mapping(target = "title", source = "sourceBook.title")
    public abstract ApiInfo from(OpenInfo source, Book sourceBook);

    protected abstract ApiContent content(Content source);
}