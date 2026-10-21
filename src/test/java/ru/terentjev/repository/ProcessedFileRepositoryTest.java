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

import ru.terentjev.repository.model.ProcessedFileEntity;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ProcessedFileRepository} and persistence of the size/modified_at
 * columns used by the book watcher against an in-memory H2 with the real Liquibase schema.
 */
@DataJdbcTest(properties = "spring.datasource.url=" + ProcessedFileRepositoryTest.DB_URL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(ProcessedFileRepositoryTest.TestConfig.class)
class ProcessedFileRepositoryTest {

    static final String DB_URL = "jdbc:h2:mem:processedfiletest;" + TestH2.OPTIONS;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @Autowired
    private ProcessedFileRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByPathReturnsSavedFile() {
        String path = "D:\\books\\book.fb2";
        repository.save(file(path, 1024L, NOW));

        Optional<ProcessedFileEntity> found = repository.findByPath(path);

        assertTrue(found.isPresent());
        assertTrue(found.get().processed());
        assertEquals("SUCCESS", found.get().result());
        assertEquals(1024L, found.get().size());
        assertEquals(NOW, found.get().modifiedAt());
    }

    @Test
    void findByPathReturnsEmptyForUnknownPath() {
        repository.save(file("D:\\books\\book.fb2", 1024L, NOW));

        assertTrue(repository.findByPath("D:\\books\\missing.fb2").isEmpty());
    }

    @Test
    void sizeAndModifiedAtMayBeNull() {
        String path = "D:\\books\\failed.fb2";
        repository.save(file(path, null, null));

        Optional<ProcessedFileEntity> found = repository.findByPath(path);

        assertTrue(found.isPresent());
        assertNull(found.get().size());
        assertNull(found.get().modifiedAt());
        assertEquals("boom", found.get().error());
    }

    @Test
    void updateKeepsIdAndOverwritesColumns() {
        String path = "D:\\books\\book.fb2";
        ProcessedFileEntity saved = repository.save(file(path, 1024L, NOW));
        ProcessedFileEntity changed = new ProcessedFileEntity(
                saved.getId(),
                path,
                true,
                "ERROR",
                "failed",
                NOW.plusSeconds(1),
                2048L,
                NOW.plusSeconds(60),
                saved.createdAt(),
                null,
                null
        );
        repository.save(changed);

        Optional<ProcessedFileEntity> found = repository.findByPath(path);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("ERROR", found.get().result());
        assertEquals(2048L, found.get().size());
        assertEquals(NOW.plusSeconds(60), found.get().modifiedAt());
    }

    private static ProcessedFileEntity file(String path, Long size, Instant modifiedAt) {
        return new ProcessedFileEntity(
                null,
                path,
                true,
                size == null ? "ERROR" : "SUCCESS",
                size == null ? "boom" : null,
                NOW,
                size,
                modifiedAt,
                null,
                null,
                null
        );
    }
}
