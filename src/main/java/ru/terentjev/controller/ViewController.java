package ru.terentjev.controller;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.terentjev.engine.pdf.PDFRender;
import ru.terentjev.service.BookService;

import java.io.*;
import java.util.List;

@Timed
@RestController
@RequestMapping("view")
public class ViewController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(ViewController.class);
    private final BookService service;

    public ViewController(BookService service) {
        super(log);
        this.service = service;
    }

    @GetMapping("test")
    public void out(HttpServletResponse response, @RequestParam int page) throws IOException {
        response.setContentType("image/png");
        ServletOutputStream os = response.getOutputStream();
        File image = new File("./pages/page_" + page + ".png");
        String name = "./books/perelman_fizika1.pdf";
        if (!image.exists()) {
            PDFRender render = new PDFRender();
            try (InputStream is = new BufferedInputStream(new FileInputStream(name))) {
                render.render(is, List.of(page, page + 1));
            }
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(image))) {
            IOUtils.copy(is, os);
        }
        os.flush();
    }

//    @PostMapping
//    public ApiExample create(@RequestBody @Valid ApiExampleDetails details) {
//        return ApiExampleMapper.mapper.from(service.save(ApiExampleMapper.mapper.create(details)));
//    }
}
