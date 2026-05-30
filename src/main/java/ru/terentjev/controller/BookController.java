package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.terentjev.controller.model.ApiInfo;
import ru.terentjev.engine.pdf.PDFRender;

import java.io.*;

@RestController
@RequestMapping("book")
public class BookController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    protected BookController() {
        super(log);
    }

    @GetMapping
    public ApiInfo info() {
        var render = new PDFRender();
        String name = "./books/perelman_fizika1.pdf";
        return run(
                () -> {
                    try (InputStream is = new BufferedInputStream(new FileInputStream(name))) {
                        return render.getInfo(is);
                    }
                },
                o -> new ApiInfo(o.getPages(), o.getAuthor(), o.getTitle())
        );
    }

}
