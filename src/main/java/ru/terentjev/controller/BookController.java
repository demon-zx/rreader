package ru.terentjev.controller;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.terentjev.service.BookService;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.render.Render;
import ru.terentjev.service.render.RenderFactory;
import ru.terentjev.service.render.RenderService;
import ru.terentjev.util.IOUtil;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Timed
@RestController
@RequestMapping("book")
public class BookController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);
    private final RenderService renderService;

    public BookController(RenderService renderService) {
        super(log);
        this.renderService = renderService;
    }

    private void out(HttpServletResponse response, InputStream is) throws IOException {
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        IOUtils.copy(is, os);
        os.flush();
    }

    @GetMapping("cover")
    public void render(HttpServletResponse response, @RequestParam UUID id) throws IOException {
        run(() -> renderService.renderCover(id, is -> out(response, is)));
    }

    @GetMapping("render")
    public void render(HttpServletResponse response, @RequestParam UUID id, @RequestParam int page) throws IOException {
        run(() -> renderService.renderPage(id, page, is -> out(response, is)));
    }

}
