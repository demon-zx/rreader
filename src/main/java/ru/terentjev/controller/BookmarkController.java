package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.terentjev.security.SecurityUtil;
import ru.terentjev.service.BookmarkService;

import io.micrometer.core.annotation.Timed;

import java.io.IOException;
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
        return run(
                () -> bookmarkService.findCurrentPosition(SecurityUtil.userRequired(), bookId),
                o -> o
        );
    }

    @PutMapping
    public void savePosition(@RequestParam UUID bookId, @RequestParam long position) throws IOException {
        run(() -> bookmarkService.saveCurrentPosition(SecurityUtil.userRequired(), bookId, position));
    }

}
