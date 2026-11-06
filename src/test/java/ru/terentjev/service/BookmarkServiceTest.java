package ru.terentjev.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.terentjev.repository.BookmarkRepository;
import ru.terentjev.repository.BookRepository;
import ru.terentjev.repository.TestH2;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.repository.model.BookmarkEntity;
import ru.terentjev.service.model.Bookmark;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тестирует {@link BookmarkService#findRecent} против in-memory H2
 * с реальной схемой Liquibase.
 */
@DataJdbcTest(properties = "spring.datasource.url=" + BookmarkServiceTest.DB_URL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import({
        BookmarkServiceTest.TestConfig.class,
        BookmarkService.class
})
class BookmarkServiceTest {
    static final String DB_URL = "jdbc:h2:mem:bookmarksvctest;" + TestH2.OPTIONS;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private BookmarkRepository repository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private BookmarkService bookmarkService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findRecentReturnsCurrentBookmarksOrderedByUpdatedAtDesc() {
        UUID bookA = bookRepository.save(book()).getId();
        UUID bookB = bookRepository.save(book()).getId();
        UUID bookC = bookRepository.save(book()).getId();
        UUID bmA = repository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookA, 1L, null, null)
        )
                .getId();
        UUID bmB = repository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookB, 2L, null, null)
        )
                .getId();
        UUID bmC = repository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookC, 3L, null, null)
        )
                .getId();
        // чужая закладка и своя «не-текущая» не должны попасть в результат
        repository.save(
                new BookmarkEntity(null, "bob", BookmarkService.CURRENT, bookA, 9L, null, null)
        );
        repository.save(
                new BookmarkEntity(null, "alice", "custom", bookA, 5L, null, null)
        );
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update("update bookmark set updated_at = ? where id = ?", base, bmA);
        jdbcTemplate.update(
                "update bookmark set updated_at = ? where id = ?",
                base.plus(1, ChronoUnit.MINUTES),
                bmB
        );
        jdbcTemplate.update(
                "update bookmark set updated_at = ? where id = ?",
                base.plus(2, ChronoUnit.MINUTES),
                bmC
        );
        List<Bookmark> recent = bookmarkService.findRecent("alice", 4);
        assertEquals(
                List.of(bookC, bookB, bookA),
                recent.stream()
                        .map(Bookmark::getBookId)
                        .toList()
        );
        assertEquals(
                List.of(3L, 2L, 1L),
                recent.stream()
                        .map(Bookmark::getPosition)
                        .toList()
        );
    }

    @Test
    void findRecentRespectsLimit() {
        UUID bookA = bookRepository.save(book()).getId();
        UUID bookB = bookRepository.save(book()).getId();
        UUID bookC = bookRepository.save(book()).getId();
        UUID bmA = repository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookA, 1L, null, null)
        )
                .getId();
        UUID bmB = repository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookB, 2L, null, null)
        )
                .getId();
        UUID bmC = repository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookC, 3L, null, null)
        )
                .getId();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update("update bookmark set updated_at = ? where id = ?", base, bmA);
        jdbcTemplate.update(
                "update bookmark set updated_at = ? where id = ?",
                base.plus(1, ChronoUnit.MINUTES),
                bmB
        );
        jdbcTemplate.update(
                "update bookmark set updated_at = ? where id = ?",
                base.plus(2, ChronoUnit.MINUTES),
                bmC
        );
        List<Bookmark> recent = bookmarkService.findRecent("alice", 2);
        assertEquals(
                List.of(bookC, bookB),
                recent.stream()
                        .map(Bookmark::getBookId)
                        .toList()
        );
    }

    private static BookEntity book() {
        return new BookEntity(
                null,
                "Title",
                "Author",
                null,
                null,
                null,
                "FB2",
                URI.create("file:///books/" + UUID.randomUUID() + ".fb2")
                        .toString(),
                "checksum",
                null,
                1L,
                null,
                null,
                null,
                null
        );
    }
}
