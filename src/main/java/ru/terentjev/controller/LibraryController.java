package ru.terentjev.controller;

import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.terentjev.controller.mapper.ApiBookMapper;
import ru.terentjev.controller.mapper.ApiBookRecentMapper;
import ru.terentjev.controller.model.ApiBook;
import ru.terentjev.controller.model.ApiBookRecent;
import ru.terentjev.controller.model.ApiPage;
import ru.terentjev.security.SecurityUtil;
import ru.terentjev.service.BookRecentService;
import ru.terentjev.service.BookService;

import java.util.List;
import java.util.UUID;

@Timed
@RestController
@RequestMapping("library")
public class LibraryController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);
    private final BookService bookService;
    private final BookRecentService bookRecentService;

    protected LibraryController(BookService bookService, BookRecentService bookRecentService) {
        super(log);
        this.bookService = bookService;
        this.bookRecentService = bookRecentService;
    }

    @GetMapping
    public ApiPage<ApiBook> findBooks(
            @RequestParam(required = false) String q,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiBookMapper.mapper.from(bookService.findAll(q, page, pageSize));
    }

    @GetMapping("recent")
    public List<ApiBookRecent> findRecentBooks(
            @RequestParam UUID deviceUuid,
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam(defaultValue = "4") int limit
    ) {
        return run(
                () -> bookRecentService.findRecent(
                        SecurityUtil.userRequired(),
                        limit,
                        deviceUuid,
                        width,
                        height
                ),
                recent -> recent.stream()
                        .map(ApiBookRecentMapper.mapper::from)
                        .toList()
        );
    }
}
