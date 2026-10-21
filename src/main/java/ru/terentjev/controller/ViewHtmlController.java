package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.terentjev.service.BookService;

import java.util.UUID;

@Controller
@RequestMapping("view")
public class ViewHtmlController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(ViewHtmlController.class);
    private final BookService bookService;

    public ViewHtmlController(BookService bookService) {
        super(log);
        this.bookService = bookService;
    }

    @GetMapping
    public String reader(Model model, @RequestParam UUID id) {
        if (id != null) {
            var book = run(() -> bookService.findById(id), o -> o);
            model.addAttribute("id", id.toString());
            model.addAttribute("title", book.getTitle());
            model.addAttribute("author", book.getAuthor());
            model.addAttribute("type", book.getType());
        }
        return "reader";
    }
}
