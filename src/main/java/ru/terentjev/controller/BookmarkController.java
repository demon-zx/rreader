package ru.terentjev.controller;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.terentjev.service.BookmarkService;
import ru.terentjev.service.render.RenderService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Timed
@RestController
@RequestMapping("bookmark")
public class BookmarkController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(BookmarkController.class);
    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        super(log);
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public long readPosition(@RequestParam UUID bookId) throws IOException {
        //TODO get current user
        return run(() -> bookmarkService.findCurrentPosition("admin", bookId), o -> o);
    }

    @PutMapping
    public void savePosition(@RequestParam UUID bookId, @RequestParam long position) throws IOException {
        //TODO get current user
        run(() -> bookmarkService.saveCurrentPosition("admin", bookId, position));
    }

}
