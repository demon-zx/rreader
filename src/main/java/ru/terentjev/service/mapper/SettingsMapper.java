package ru.terentjev.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.SettingsEntity;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.SettingsScope;

import java.time.Instant;
import java.util.UUID;

@Mapper(config = Config.class)
public abstract class SettingsMapper {
    public static SettingsMapper mapper = Mappers.getMapper(SettingsMapper.class);

    public abstract KeyValue from(SettingsEntity source);

    @Mapping(target = "id", ignore = true)
    public abstract SettingsEntity to(
            KeyValue source,
            UUID deviceUuid,
            String scope,
            Instant updatedAt,
            String updatedBy
    );

    public SettingsScope scope(String source) {
        try {
            return SettingsScope.valueOf(source);
        } catch (IllegalArgumentException e) {
            return SettingsScope.GLOBAL;
        }
    }
}
