package ru.terentjev.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.terentjev.repository.model.SettingsEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettingsRepository
        extends ListCrudRepository<SettingsEntity, UUID> {

    List<SettingsEntity> findByDeviceUuidAndScopeIn(UUID deviceUuid, List<String> scopes);

    @Modifying
    @Query("delete from settings where device_uuid = :deviceUuid and scope = :scope")
    void deleteByDeviceUuidAndScope(
            @Param("deviceUuid") UUID deviceUuid,
            @Param("scope") String scope
    );

}
