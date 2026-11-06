package ru.terentjev.service;

import org.springframework.stereotype.Service;

import ru.terentjev.exception.ServiceException;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.BookRecent;
import ru.terentjev.service.model.Bookmark;
import ru.terentjev.service.render.OpenInfo;
import ru.terentjev.service.render.RenderService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Книги, которые читают сейчас: у которых есть «текущая» закладка,
 * с текущей позицией чтения и процентом прочитанного.
 */
@Service
public class BookRecentService {
    private final BookmarkService bookmarkService;
    private final BookService bookService;
    private final RenderService renderService;

    public BookRecentService(
            BookmarkService bookmarkService,
            BookService bookService,
            RenderService renderService
    ) {
        this.bookmarkService = bookmarkService;
        this.bookService = bookService;
        this.renderService = renderService;
    }

    /**
     * Книги, читаемые сейчас (с «текущей» закладкой), с текущей позицией
     * чтения и процентом прочитанного, отсортированные по дате изменения
     * закладки по убыванию (не более {@code limit} записей).
     */
    public List<BookRecent> findRecent(
            String username,
            int limit,
            UUID deviceUuid,
            int width,
            int height
    ) throws ServiceException, IOException {
        Map<UUID, Long> positions = new LinkedHashMap<>();
        for (Bookmark bookmark : bookmarkService.findRecent(username, limit)) {
            positions.put(bookmark.getBookId(), bookmark.getPosition());
        }
        List<BookRecent> result = new ArrayList<>(positions.size());
        for (Book book : bookService.findByIds(positions.keySet())) {
            long position = positions.get(book.getId());
            OpenInfo info = renderService.open(book.getId(), deviceUuid, width, height);
            int pages = info.getPages();
            int percent = percent(position, pages, info.getPositionByPage());
            result.add(new BookRecent(book, position, pages, percent));
        }
        return result;
    }

    private static int percent(long position, int pages, Map<Integer, Integer> positionByPage) {
        if (pages <= 0) {
            return 0;
        }
        int page = pageForPosition(position, positionByPage);
        return Math.min(100, page * 100 / pages);
    }

    /**
     * Номер страницы, соответствующий позиции сервера. Если соответствие
     * «страница → позиция» пустое (PDF), номер страницы равен позиции.
     */
    private static int pageForPosition(long position, Map<Integer, Integer> positionByPage) {
        if (positionByPage == null || positionByPage.isEmpty()) {
            return Math.max(1, (int) position);
        }
        int page = 1;
        int best = -1;
        for (Map.Entry<Integer, Integer> e : positionByPage.entrySet()) {
            if (e.getValue() <= position && e.getValue() > best) {
                best = e.getValue();
                page = e.getKey();
            }
        }
        return page;
    }
}
