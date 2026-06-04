package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.BookmarkEntity;
import ru.terentjev.service.model.Bookmark;

@Mapper(config = Config.class)
public abstract class BookmarkMapper extends AbstractPageMapper<Bookmark, BookmarkEntity> {
    public static BookmarkMapper mapper = Mappers.getMapper(BookmarkMapper.class);

    public abstract Bookmark from(BookmarkEntity source);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @InheritInverseConfiguration
    public abstract BookmarkEntity to(Bookmark source);

}