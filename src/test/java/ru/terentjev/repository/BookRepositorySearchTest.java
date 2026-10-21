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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.terentjev.repository.model.BookEntity;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link BookRepositorySearch} fragment against an in-memory H2 with the real
 * Liquibase schema. Note the embedded datasource has no {@code IGNORECASE} flag, so any
 * case-insensitive matching proven here comes from {@code Criteria.ignoreCase(true)}, not the URL.
 */
@DataJdbcTest(properties = "spring.datasource.url=" + BookRepositorySearchTest.DB_URL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(BookRepositorySearchTest.TestConfig.class)
class BookRepositorySearchTest {

    static final String DB_URL = "jdbc:h2:mem:searchtest;" + TestH2.OPTIONS;

    /**
     * The slice pulls in {@code JdbcConfiguration} (an {@code AbstractJdbcConfiguration}) but not
     * {@code MainConfiguration}, so its {@link Clock} dependency has to be supplied here.
     */
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
        repository.save(book("Isaac Asimov", "Foundation", "Foundation Series"));
        repository.save(book("Arthur Clarke", "Rendezvous", null));
        repository.save(book("Ivan Efremov", "Andromeda", "Great Ring"));
    }

    @Test
    void matchesAuthorIgnoringCase() {
        Page<BookEntity> result = repository.search("asimov", PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("Isaac Asimov", result.getContent().get(0).author());
    }

    @Test
    void matchesTitleIgnoringCase() {
        Page<BookEntity> result = repository.search("RENDEZVOUS", PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("Arthur Clarke", result.getContent().get(0).author());
    }

    @Test
    void matchesSequenceName() {
        Page<BookEntity> result = repository.search("ring", PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("Andromeda", result.getContent().get(0).title());
    }

    @Test
    void paginatesAndCountsAcrossAllColumns() {
        Pageable firstPage = PageRequest.of(0, 2, Sort.by("author").ascending());

        Page<BookEntity> result = repository.search("a", firstPage);

        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        List<String> authors = result.getContent().stream().map(BookEntity::author).toList();
        assertIterableEquals(List.of("Arthur Clarke", "Isaac Asimov"), authors);
    }

    @Test
    void returnsEmptyWhenNothingMatches() {
        Page<BookEntity> result = repository.search("zzz", PageRequest.of(0, 10));

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    private static BookEntity book(String author, String title, String sequenceName) {
        return new BookEntity(
                null, title, author, "en", sequenceName, null,
                "FB2", "/books/" + title + ".fb2", title + "-checksum",
                null, 100L, null, null, null, null
        );
    }
}
