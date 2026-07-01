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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@link BookRepositorySearch} fragment against an in-memory H2 with the real
 * Liquibase schema. Note the embedded datasource has no {@code IGNORECASE} flag, so any
 * case-insensitive matching proven here comes from {@code Criteria.ignoreCase(true)}, not the URL.
 */
@DataJdbcTest(properties =
        "spring.datasource.url=jdbc:h2:mem:searchtest;IGNORECASE=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(BookRepositorySearchTest.TestConfig.class)
class BookRepositorySearchTest {

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

        assertThat(result.getContent())
                .singleElement()
                .extracting(BookEntity::author)
                .isEqualTo("Isaac Asimov");
    }

    @Test
    void matchesTitleIgnoringCase() {
        Page<BookEntity> result = repository.search("RENDEZVOUS", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .singleElement()
                .extracting(BookEntity::author)
                .isEqualTo("Arthur Clarke");
    }

    @Test
    void matchesSequenceName() {
        Page<BookEntity> result = repository.search("ring", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .singleElement()
                .extracting(BookEntity::title)
                .isEqualTo("Andromeda");
    }

    @Test
    void paginatesAndCountsAcrossAllColumns() {
        Pageable firstPage = PageRequest.of(0, 2, Sort.by("author").ascending());

        Page<BookEntity> result = repository.search("a", firstPage);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BookEntity::author)
                .containsExactly("Arthur Clarke", "Isaac Asimov");
    }

    @Test
    void returnsEmptyWhenNothingMatches() {
        Page<BookEntity> result = repository.search("zzz", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private static BookEntity book(String author, String title, String sequenceName) {
        return new BookEntity(
                null, title, author, "en", sequenceName, null,
                "FB2", "/books/" + title + ".fb2", title + "-checksum",
                null, 100L, null, null, null, null
        );
    }
}
