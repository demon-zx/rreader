package ru.terentjev.controller;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.terentjev.controller.mapper.ApiInfoMapper;
import ru.terentjev.controller.model.ApiInfo;
import ru.terentjev.service.BookService;
import ru.terentjev.service.render.RenderService;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Timed
@RestController
@RequestMapping("book")
public class BookController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);
    private final RenderService renderService;
    private final BookService bookService;

    public BookController(
            RenderService renderService,
            BookService bookService
    ) {
        super(log);
        this.renderService = renderService;
        this.bookService = bookService;
    }

    private void out(HttpServletResponse response, InputStream is) throws IOException {
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "private,max-age=2592000");
        OutputStream os = response.getOutputStream();
        IOUtils.copy(is, os);
        os.flush();
    }

    @GetMapping("{id}")
    public ApiInfo open(
            @PathVariable UUID id,
            @RequestParam UUID deviceUuid,
            @RequestParam int width,
            @RequestParam int height
    ) {
        return run(
            () -> {
                log.debug("info id={} deviceUuid={} viewport={}x{}", id, deviceUuid, width, height);
                var book = bookService.findById(id);
                var info = renderService.open(id, deviceUuid, width, height);
                return ApiInfoMapper.mapper.from(info, book);
            },
            b -> b
        );
    }
    

    @GetMapping("{id}/cover")
    public void cover(HttpServletResponse response, @PathVariable UUID id, @RequestParam UUID deviceUuid) throws IOException {
        run(() -> renderService.renderCover(id, deviceUuid, is -> out(response, is)));
    }

    @GetMapping("{id}/render/{page}")
    public void render(
            HttpServletResponse response,
            @PathVariable UUID id,
            @PathVariable int page,
            @RequestParam UUID deviceUuid,
            @RequestParam int width,
            @RequestParam int height
    ) throws IOException {
        run(() -> renderService.renderPage(id, page, deviceUuid, width, height, is -> out(response, is)));
    }

}
