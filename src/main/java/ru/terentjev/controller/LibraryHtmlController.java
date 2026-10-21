package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.terentjev.service.BookService;

@Controller
public class LibraryHtmlController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(LibraryHtmlController.class);
    private final BookService bookService;

    public LibraryHtmlController(BookService bookService) {
        super(log);
        this.bookService = bookService;
    }

    @GetMapping
    public String library(Model model) {
        model.addAttribute("all", bookService.count());
        return "library";
    }
}
