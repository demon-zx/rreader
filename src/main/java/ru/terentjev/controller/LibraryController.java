package ru.terentjev.controller;

import org.apache.commons.lang3.StringUtils;
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
import ru.terentjev.service.model.Book;

import java.util.List;

@RestController
@RequestMapping("library")
public class LibraryController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);

    private final BookService bookService;

    protected LibraryController(BookService bookService) {
        super(log);
        this.bookService = bookService;
    }

//    @GetMapping
//    public ApiInfo info() {
//        var render = new PDFRender();
//        String name = "./books/perelman_fizika1.pdf";
//        return run(
//                () -> {
//                    try (InputStream is = new BufferedInputStream(new FileInputStream(name))) {
//                        return render.info(is);
//                    }
//                },
//                o -> ApiInfoMapper.mapper.from(o)
//        );
//    }

    @GetMapping
    public ApiPage<ApiBook> findBooks(
            @RequestParam(required = false) String q,
            @RequestParam int from,
            @RequestParam int limit
    ) {
        return ApiBookMapper.mapper.from(bookService.findAll(q, from, limit));
    }
}
