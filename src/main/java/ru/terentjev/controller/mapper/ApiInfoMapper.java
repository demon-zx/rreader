package ru.terentjev.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.terentjev.controller.model.ApiInfo;
import ru.terentjev.service.render.Info;

@Mapper(config = Config.class)
public abstract class ApiInfoMapper {
    public static ApiInfoMapper mapper = Mappers.getMapper(ApiInfoMapper.class);

    public abstract ApiInfo from(Info source);

}