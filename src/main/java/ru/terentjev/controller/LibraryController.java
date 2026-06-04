package ru.terentjev.controller;

import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.terentjev.controller.mapper.ApiBookMapper;
import ru.terentjev.controller.model.ApiBook;
import ru.terentjev.controller.model.ApiPage;
import ru.terentjev.service.BookService;

@Timed
@RestController
@RequestMapping("library")
public class LibraryController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);

    private final BookService bookService;

    protected LibraryController(BookService bookService) {
        super(log);
        this.bookService = bookService;
    }

    @GetMapping
    public ApiPage<ApiBook> findBooks(
            @RequestParam(required = false) String q,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiBookMapper.mapper.from(bookService.findAll(q, page, pageSize));
    }

}
