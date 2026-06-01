package ru.terentjev.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.service.model.Book;

import java.nio.file.Path;

@Mapper(config = Config.class)
public abstract class PathMapper {
    public static PathMapper mapper = Mappers.getMapper(PathMapper.class);

    public Path from(String source) {
        if (source == null) {
            return null;
        }
        return Path.of(source);
    }


    public String to(Path source) {
        if (source == null) {
            return null;
        }
        return source.toString();
    }

}