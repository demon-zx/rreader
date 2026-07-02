package ru.terentjev.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.terentjev.repository.model.ProcessedFileEntity;
import ru.terentjev.service.model.ProcessedFile;

@Mapper(config = Config.class, uses = PathMapper.class)
public abstract class ProcessedFileMapper {
    public static ProcessedFileMapper mapper = Mappers.getMapper(ProcessedFileMapper.class);

    public abstract ProcessedFile from(ProcessedFileEntity source);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @InheritInverseConfiguration
    public abstract ProcessedFileEntity to(ProcessedFile source);

}
