package ru.terentjev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.BookRecent;
import ru.terentjev.service.model.Bookmark;
import ru.terentjev.service.render.OpenInfo;
import ru.terentjev.service.render.RenderService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookRecentServiceTest {
    private BookmarkService bookmarkService;
    private BookService bookService;
    private RenderService renderService;
    private BookRecentService service;
    private UUID device;

    @BeforeEach
    void setUp() {
        bookmarkService = mock(BookmarkService.class);
        bookService = mock(BookService.class);
        renderService = mock(RenderService.class);
        service = new BookRecentService(bookmarkService, bookService, renderService);
        device = UUID.randomUUID();
    }

    @Test
    void findRecentReturnsBooksInOrderWithPositionAndPercent() throws Exception {
        UUID bookA = UUID.randomUUID();
        UUID bookB = UUID.randomUUID();
        when(bookmarkService.findRecent("alice", 4)).thenReturn(
                List.of(
                        new Bookmark(null, "alice", BookmarkService.CURRENT, bookA, 1500L),
                        new Bookmark(null, "alice", BookmarkService.CURRENT, bookB, 3L)
                )
        );
        when(bookService.findByIds(argThat(ids -> ids.equals(Set.of(bookA, bookB)))))
                .thenReturn(List.of(book(bookA), book(bookB)));
        when(renderService.open(eq(bookA), eq(device), eq(800), eq(600))).thenReturn(
                new OpenInfo("Author A", "Title A", 10, null, null, Map.of(1, 0, 5, 1000, 10, 2000))
        );
        when(renderService.open(eq(bookB), eq(device), eq(800), eq(600))).thenReturn(
                new OpenInfo("Author B", "Title B", 10, null, null, Map.of())
        );
        List<BookRecent> recent = service.findRecent("alice", 4, device, 800, 600);
        assertEquals(2, recent.size());
        assertEquals(bookA, recent.get(0).getId());
        assertEquals(1500L, recent.get(0).getPosition());
        assertEquals(10, recent.get(0).getPages());
        // позиция 1500 → страница 5 (позиция 1000 <= 1500, 2000 > 1500)
        assertEquals(50, recent.get(0).getPercent());
        assertEquals(bookB, recent.get(1).getId());
        assertEquals(3L, recent.get(1).getPosition());
        // PDF: без соответствия «страница → позиция» номер страницы равен позиции
        assertEquals(30, recent.get(1).getPercent());
        verify(bookmarkService).findRecent("alice", 4);
        verify(bookService).findByIds(Set.of(bookA, bookB));
    }

    @Test
    void findRecentClampsPercentAt100() throws Exception {
        UUID bookA = UUID.randomUUID();
        when(bookmarkService.findRecent("alice", 4)).thenReturn(
                List.of(new Bookmark(null, "alice", BookmarkService.CURRENT, bookA, 2000L))
        );
        when(bookService.findByIds(Set.of(bookA))).thenReturn(List.of(book(bookA)));
        when(renderService.open(eq(bookA), eq(device), eq(800), eq(600))).thenReturn(
                new OpenInfo("Author A", "Title A", 10, null, null, Map.of(1, 0, 10, 2000))
        );
        List<BookRecent> recent = service.findRecent("alice", 4, device, 800, 600);
        assertEquals(100, recent.get(0).getPercent());
    }

    @Test
    void findRecentReturnsFirstPagePercentForEarlyPosition() throws Exception {
        UUID bookA = UUID.randomUUID();
        when(bookmarkService.findRecent("alice", 4)).thenReturn(
                List.of(new Bookmark(null, "alice", BookmarkService.CURRENT, bookA, 0L))
        );
        when(bookService.findByIds(Set.of(bookA))).thenReturn(List.of(book(bookA)));
        when(renderService.open(eq(bookA), eq(device), eq(800), eq(600))).thenReturn(
                new OpenInfo("Author A", "Title A", 10, null, null, Map.of(1, 0, 5, 1000, 10, 2000))
        );
        List<BookRecent> recent = service.findRecent("alice", 4, device, 800, 600);
        // позиция 0 не дальше первой страницы
        assertEquals(10, recent.get(0).getPercent());
    }

    @Test
    void findRecentReturnsZeroPercentWithoutPages() throws Exception {
        UUID bookA = UUID.randomUUID();
        when(bookmarkService.findRecent("alice", 4)).thenReturn(
                List.of(new Bookmark(null, "alice", BookmarkService.CURRENT, bookA, 1L))
        );
        when(bookService.findByIds(Set.of(bookA))).thenReturn(List.of(book(bookA)));
        when(renderService.open(eq(bookA), eq(device), eq(800), eq(600))).thenReturn(
                new OpenInfo("Author A", "Title A", 0, null, null, Map.of())
        );
        List<BookRecent> recent = service.findRecent("alice", 4, device, 800, 600);
        assertEquals(0, recent.get(0).getPercent());
    }

    @Test
    void findRecentSkipsBooksNotFound() throws Exception {
        UUID bookA = UUID.randomUUID();
        UUID bookB = UUID.randomUUID();
        when(bookmarkService.findRecent("alice", 4)).thenReturn(
                List.of(
                        new Bookmark(null, "alice", BookmarkService.CURRENT, bookA, 1L),
                        new Bookmark(null, "alice", BookmarkService.CURRENT, bookB, 2L)
                )
        );
        when(bookService.findByIds(Set.of(bookA, bookB))).thenReturn(List.of(book(bookA)));
        when(renderService.open(eq(bookA), eq(device), eq(800), eq(600))).thenReturn(
                new OpenInfo("Author A", "Title A", 10, null, null, Map.of())
        );
        List<BookRecent> recent = service.findRecent("alice", 4, device, 800, 600);
        assertEquals(1, recent.size());
        assertEquals(bookA, recent.get(0).getId());
        verify(renderService, never()).open(eq(bookB), any(), anyInt(), anyInt());
    }

    @Test
    void findRecentReturnsEmptyListWhenNoBookmarks() throws Exception {
        when(bookmarkService.findRecent("bob", 4)).thenReturn(List.of());
        when(bookService.findByIds(Set.of())).thenReturn(List.of());
        List<BookRecent> recent = service.findRecent("bob", 4, device, 800, 600);
        assertTrue(recent.isEmpty());
        verify(renderService, never()).open(any(), any(), anyInt(), anyInt());
    }

    private static Book book(UUID id) {
        return new Book(
                id,
                "Title",
                "Author",
                null,
                null,
                null,
                "FB2",
                URI.create("file:///books/" + id + ".fb2"),
                "checksum",
                null,
                100L
        );
    }
}
