package ru.terentjev.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.net.URI;

@Mapper(config = Config.class)
public abstract class UriMapper {
    public static UriMapper mapper = Mappers.getMapper(UriMapper.class);

    public URI from(String source) {
        if (source == null) {
            return null;
        }
        return URI.create(source);
    }

    public String to(URI source) {
        if (source == null) {
            return null;
        }
        return source.toString();
    }
}
