package ru.terentjev.controller;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
        return run(() -> bookmarkService.findCurrentPosition(currentUser(), bookId), o -> o);
    }

    @PutMapping
    public void savePosition(@RequestParam UUID bookId, @RequestParam long position) throws IOException {
        run(() -> bookmarkService.saveCurrentPosition(currentUser(), bookId, position));
    }

    public String currentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new IllegalStateException("User not found");
    }

}
