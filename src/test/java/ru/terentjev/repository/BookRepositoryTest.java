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

import ru.terentjev.repository.model.BookEntity;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link BookRepository} query methods against an in-memory H2 with the real
 * Liquibase schema.
 */
@DataJdbcTest(properties = "spring.datasource.url=" + BookRepositoryTest.DB_URL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(BookRepositoryTest.TestConfig.class)
class BookRepositoryTest {

    static final String DB_URL = "jdbc:h2:mem:bookrepotest;" + TestH2.OPTIONS;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private BookRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByUriReturnsSavedBook() {
        String uri = "file:///books/foundation.fb2";
        BookEntity saved = repository.save(book(uri));

        Optional<BookEntity> found = repository.findByUri(uri);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(uri, found.get().uri());
    }

    @Test
    void findByUriReturnsEmptyForUnknownUri() {
        repository.save(book("file:///books/foundation.fb2"));

        assertTrue(repository.findByUri("file:///books/missing.fb2").isEmpty());
    }

    @Test
    void findAllAsStreamStreamsAllBooks() {
        repository.save(book("file:///books/1.fb2"));
        repository.save(book("file:///books/2.fb2"));
        repository.save(book("file:///books/3.fb2"));

        List<String> uris;
        try (var stream = repository.findAllAsStream()) {
            uris = stream.map(BookEntity::uri).toList();
        }

        List<String> expected = new ArrayList<>(List.of(
                "file:///books/1.fb2", "file:///books/2.fb2", "file:///books/3.fb2"
        ));
        List<String> actual = new ArrayList<>(uris);
        Collections.sort(expected);
        Collections.sort(actual);
        assertIterableEquals(expected, actual);
    }

    @Test
    void findAllAsStreamReturnsEmptyStreamWithoutBooks() {
        try (var stream = repository.findAllAsStream()) {
            assertTrue(stream.toList().isEmpty());
        }
    }

    private static BookEntity book(String uri) {
        return new BookEntity(
                null, "Title", "Author", null, null, null,
                "FB2", uri, "checksum", null, 100L,
                null, null, null, null
        );
    }
}
