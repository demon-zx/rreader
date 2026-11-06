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

import ru.terentjev.repository.BookRepository;
import ru.terentjev.repository.TestH2;
import ru.terentjev.repository.model.BookEntity;
import ru.terentjev.service.model.Book;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тестирует {@link BookService#findByIds} против in-memory H2
 * с реальной схемой Liquibase.
 */
@DataJdbcTest(properties = "spring.datasource.url=" + BookServiceTest.DB_URL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import({
        BookServiceTest.TestConfig.class,
        BookService.class
})
class BookServiceTest {
    static final String DB_URL = "jdbc:h2:mem:booksvcbatchtest;" + TestH2.OPTIONS;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private BookRepository repository;
    @Autowired
    private BookService bookService;

    @Test
    void findByIdsPreservesOrderAndSkipsMissing() {
        UUID bookA = repository.save(book()).getId();
        UUID bookB = repository.save(book()).getId();
        UUID bookC = repository.save(book()).getId();
        UUID missing = UUID.randomUUID();
        List<Book> books = bookService.findByIds(List.of(bookB, missing, bookA));
        assertEquals(
                List.of(bookB, bookA),
                books.stream()
                        .map(Book::getId)
                        .toList()
        );
        assertEquals("Author", books.get(0).getAuthor());
    }

    @Test
    void findByIdsReturnsEmptyListWithoutIds() {
        repository.save(book());
        assertTrue(
                bookService.findByIds(List.of())
                        .isEmpty()
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
