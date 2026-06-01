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
import ru.terentjev.util.IOUtil;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Timed
@RestController
@RequestMapping("view")
public class ViewController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(ViewController.class);
    private final BookService service;
    private final RenderFactory renderFactory;

    public ViewController(BookService service, RenderFactory renderFactory) {
        super(log);
        this.service = service;
        this.renderFactory = renderFactory;
    }

    @GetMapping("render")
    public void render(HttpServletResponse response, @RequestParam UUID id, @RequestParam int page) throws IOException {
        Book book = run(() -> service.findById(id), o -> o);
        response.setContentType("image/png");
        ServletOutputStream os = response.getOutputStream();
        Path renderedPage = pathPage(id, page);
        if (Files.notExists(renderedPage)) {
            render(page, book);
        }
        try (InputStream is = new BufferedInputStream(Files.newInputStream(renderedPage))) {
            IOUtils.copy(is, os);
        }
        os.flush();
    }

    private void render(int page, Book book) throws IOException {
        Render render = renderFactory.getRender(book.getType());
        IOUtil.readFile(
                book.getPath(),
                is -> {
                    render.render(
                            is, (renderedPage, numberPage) -> {
                                Path output = pathPage(book.getId(), numberPage);
                                if (Files.notExists(output.getParent())) {
                                    Files.createDirectories(output.getParent());
                                }
                                ImageIO.write(renderedPage, "PNG", output.toFile());

                            }, List.of(page, page + 1)
                    );
                    return null;
                }
        );
    }

    private Path pathPage(UUID id, int page) {
        return Path.of("pages", id.toString(), page + ".png");
    }
//    @PostMapping
//    public ApiExample create(@RequestBody @Valid ApiExampleDetails details) {
//        return ApiExampleMapper.mapper.from(service.save(ApiExampleMapper.mapper.create(details)));
//    }
}
