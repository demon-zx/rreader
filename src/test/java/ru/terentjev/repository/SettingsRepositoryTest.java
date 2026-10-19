package ru.terentjev.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import ru.terentjev.repository.model.SettingsEntity;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the settings table and {@link SettingsRepository} against an in-memory H2
 * with the real Liquibase schema.
 */
@DataJdbcTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:settingstest;IGNORECASE=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "logging.level.org.springframework.jdbc.core.JdbcTemplate=debug"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(SettingsRepositoryTest.TestConfig.class)
class SettingsRepositoryTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    private final UUID deviceUuid = UUID.randomUUID();

    @Autowired
    private SettingsRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void savesAndFindsByScopeHierarchy() {
        repository.save(setting("GLOBAL", "font", "Arial"));
        repository.save(setting("GLOBAL", "size", "28"));
        repository.save(setting("FB2", "font", "Georgia"));

        List<SettingsEntity> settings = repository.findByDeviceUuidAndScopeIn(
                deviceUuid, List.of("FB2", "GLOBAL"));

        assertThat(settings).hasSize(3);
    }

    @Test
    void findsOnlyRequestedScopes() {
        repository.save(setting("GLOBAL", "font", "Arial"));
        repository.save(setting("PDF", "font", "Times New Roman"));

        List<SettingsEntity> settings = repository.findByDeviceUuidAndScopeIn(
                deviceUuid, List.of("FB2", "GLOBAL"));

        assertThat(settings).hasSize(1);
        assertThat(settings).first().satisfies(e -> {
            assertThat(e.scope()).isEqualTo("GLOBAL");
            assertThat(e.value()).isEqualTo("Arial");
        });
    }

    @Test
    void deleteByScopeRemovesOnlyMatchingScope() {
        repository.save(setting("GLOBAL", "font", "Arial"));
        repository.save(setting("FB2", "font", "Georgia"));

        repository.deleteByDeviceUuidAndScope(deviceUuid, "GLOBAL");

        assertThat(repository.findByDeviceUuidAndScopeIn(deviceUuid, List.of("GLOBAL"))).isEmpty();
        assertThat(repository.findByDeviceUuidAndScopeIn(deviceUuid, List.of("FB2"))).hasSize(1);
    }

    private SettingsEntity setting(String scope, String key, String value) {
        return new SettingsEntity(null, deviceUuid, scope, key, value, null, null);
    }
}
