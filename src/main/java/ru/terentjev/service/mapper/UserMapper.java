package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.UserEntity;
import ru.terentjev.service.model.User;

@Mapper(config = Config.class)
public abstract class UserMapper {
    public static UserMapper mapper = Mappers.getMapper(UserMapper.class);

    public abstract User from(UserEntity source);

    @InheritInverseConfiguration
    public abstract UserEntity to(User source);
}
