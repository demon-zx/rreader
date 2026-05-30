package ru.terentjev.controller.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ru.terentjev.controller.model.ApiExample;
import ru.terentjev.controller.model.ApiExampleDetails;
import ru.terentjev.service.model.Example;

@Mapper(config = Config.class)
public abstract class ApiExampleMapper {
    public static ApiExampleMapper mapper = Mappers.getMapper(ApiExampleMapper.class);

    public abstract ApiExample from(Example source);

    @Mapping(target = "id", constant = "0")
    public abstract Example create(ApiExampleDetails source);

    @InheritInverseConfiguration
    public abstract Example to(ApiExample source);

}