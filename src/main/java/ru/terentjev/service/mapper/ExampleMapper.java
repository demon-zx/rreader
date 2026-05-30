package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ru.terentjev.repository.model.ExampleEntity;
import ru.terentjev.service.model.Example;

@Mapper(config = Config.class)
public abstract class ExampleMapper {
    public static ExampleMapper mapper = Mappers.getMapper(ExampleMapper.class);

    public abstract Example from(ExampleEntity source);

    @InheritInverseConfiguration
    public abstract ExampleEntity to(Example source);

}