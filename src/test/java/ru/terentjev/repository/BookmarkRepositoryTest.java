package ru.terentjev.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.repository.model.BookmarkEntity;
import ru.terentjev.service.BookService;
import ru.terentjev.service.BookmarkService;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.Bookmark;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJdbcTest(properties = {
        "spring.datasource.url=" + BookmarkRepositoryTest.DB_URL,
        "logging.level.org.springframework.jdbc.core.JdbcTemplate=debug"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import({
        BookmarkRepositoryTest.TestConfig.class, BookmarkService.class, BookService.class
})
class BookmarkRepositoryTest {
    static final String DB_URL = "jdbc:h2:mem:bookmarktest;" + TestH2.OPTIONS;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private BookmarkRepository bookmarkRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private BookmarkService bookmarkService;
    @Autowired
    private BookService bookService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteByBookIdRemovesBookmarks() {
        UUID bookId = bookRepository.save(book())
                .getId();
        bookmarkService.save(new Bookmark(null, "alice", BookmarkService.CURRENT, bookId, 3L));
        bookmarkService.save(new Bookmark(null, "bob", BookmarkService.CURRENT, bookId, 7L));
        UUID otherId = bookRepository.save(book())
                .getId();
        bookmarkService.save(new Bookmark(null, "alice", BookmarkService.CURRENT, otherId, 1L));
        bookmarkRepository.deleteByBookId(bookId);
        assertEquals(1, bookmarkRepository.count());
        assertEquals(
                otherId,
                bookmarkRepository.findAll()
                        .iterator()
                        .next()
                        .bookId()
        );
    }

    @Test
    void findCurrentOrdersByUpdatedAtDescRespectsUserAndLimit() {
        UUID bookA = bookRepository.save(book()).getId();
        UUID bookB = bookRepository.save(book()).getId();
        UUID bookC = bookRepository.save(book()).getId();
        UUID bmA = bookmarkRepository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookA, 1L, null, null)
        )
                .getId();
        UUID bmB = bookmarkRepository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookB, 2L, null, null)
        )
                .getId();
        UUID bmC = bookmarkRepository.save(
                new BookmarkEntity(null, "alice", BookmarkService.CURRENT, bookC, 3L, null, null)
        )
                .getId();
        bookmarkRepository.save(
                new BookmarkEntity(null, "bob", BookmarkService.CURRENT, bookA, 1L, null, null)
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
        List<BookmarkEntity> current = bookmarkRepository.findCurrent(
                "alice",
                BookmarkService.CURRENT,
                PageRequest.of(0, 2, Sort.by("updatedAt").descending())
        );
        assertEquals(List.of(bmC, bmB), current.stream().map(BookmarkEntity::id).toList());
        assertEquals(
                List.of(bookC, bookB, bookA),
                bookmarkService.findRecent("alice", 4)
                        .stream()
                        .map(Bookmark::getBookId)
                        .toList()
        );
    }

    @Test
    void watcherCascadeFlowDeletesBookWithBookmarks() throws Exception {
        URI uri = URI.create("file:///books/deleteme.fb2");
        UUID bookId = bookRepository.save(book(uri))
                .getId();
        bookmarkService.save(new Bookmark(null, "alice", BookmarkService.CURRENT, bookId, 3L));
        List<Book> removed = bookService.findByUriContaining(uri.toString());
        for (Book book : removed) {
            bookmarkService.deleteByBook(book.getId());
            bookService.delete(book.getId());
        }
        assertTrue(
                bookRepository.findById(bookId)
                        .isEmpty()
        );
        assertEquals(0, bookmarkRepository.count());
    }

    @Test
    void findByUriContainingMatchesBooksInsideZipArchive() {
        String archive = "file:///D:/books/Весь%20Азимов.zip";
        UUID innerId = bookRepository.save(
                book(
                        URI.create(
                                "zip:" + archive + "!/Весь%20АЗИМОВ.%20ТОМ%201%20«Я,%20робот».fb2"
                        )
                )
        )
                .getId();
        bookRepository.save(
                book(
                        URI.create(
                                "zip:file:///D:/books/Весь%20Азимов2.zip!/другая.fb2"
                        )
                )
        );
        List<Book> removed = bookService.findByUriContaining(archive);
        assertEquals(1, removed.size());
        assertEquals(
                innerId,
                removed.get(0)
                        .getId()
        );
    }

    private static BookEntity book() {
        return book(URI.create("file:///books/" + UUID.randomUUID() + ".fb2"));
    }

    private static BookEntity book(URI uri) {
        return new BookEntity(
                null,
                "Title",
                "Author",
                null,
                null,
                null,
                "FB2",
                uri.toString(),
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
