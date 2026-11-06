package ru.terentjev.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.terentjev.service.BookRecentService;
import ru.terentjev.service.BookService;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.BookRecent;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(LibraryControllerTest.TestConfig.class)
class LibraryControllerTest {
    static class TestConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private static final UUID DEVICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private BookService bookService;
    @MockitoBean
    private BookRecentService bookRecentService;

    @Test
    void recentReturnsBooksWithReadingProgress() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(bookRecentService.findRecent("alice", 4, DEVICE, 800, 600)).thenReturn(
                List.of(
                        new BookRecent(
                                book(bookId),
                                1500L,
                                10,
                                50
                        )
                )
        );
        mockMvc.perform(
                get("/api/v1/library/recent")
                        .with(authenticatedUser("alice"))
                        .param("deviceUuid", DEVICE.toString())
                        .param("width", "800")
                        .param("height", "600")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookId.toString()))
                .andExpect(jsonPath("$[0].title").value("Title"))
                .andExpect(jsonPath("$[0].author").value("Author"))
                .andExpect(jsonPath("$[0].size").value("100"))
                .andExpect(jsonPath("$[0].position").value(1500))
                .andExpect(jsonPath("$[0].pages").value(10))
                .andExpect(jsonPath("$[0].percent").value(50));
    }

    @Test
    void recentPassesLimitToService() throws Exception {
        when(bookRecentService.findRecent("alice", 2, DEVICE, 800, 600)).thenReturn(List.of());
        mockMvc.perform(
                get("/api/v1/library/recent")
                        .with(authenticatedUser("alice"))
                        .param("deviceUuid", DEVICE.toString())
                        .param("width", "800")
                        .param("height", "600")
                        .param("limit", "2")
        )
                .andExpect(status().isOk());
        verify(bookRecentService).findRecent("alice", 2, DEVICE, 800, 600);
    }

    @Test
    void recentReturnsEmptyArrayWhenNoRecentBooks() throws Exception {
        when(bookRecentService.findRecent("alice", 4, DEVICE, 800, 600)).thenReturn(List.of());
        mockMvc.perform(
                get("/api/v1/library/recent")
                        .with(authenticatedUser("alice"))
                        .param("deviceUuid", DEVICE.toString())
                        .param("width", "800")
                        .param("height", "600")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recentReturns400WhenDeviceUuidMissing() throws Exception {
        mockMvc.perform(
                get("/api/v1/library/recent")
                        .with(authenticatedUser("alice"))
                        .param("width", "800")
                        .param("height", "600")
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void recentReturns500WhenServiceFails() throws Exception {
        when(bookRecentService.findRecent(eq("alice"), anyInt(), eq(DEVICE), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("boom"));
        mockMvc.perform(
                get("/api/v1/library/recent")
                        .with(authenticatedUser("alice"))
                        .param("deviceUuid", DEVICE.toString())
                        .param("width", "800")
                        .param("height", "600")
        )
                .andExpect(status().isInternalServerError());
    }

    private static Book book(UUID id) {
        return new Book(
                id,
                "Title",
                "Author",
                null,
                null,
                null,
                "FB2",
                URI.create("file:///books/" + id + ".fb2"),
                "checksum",
                null,
                100L
        );
    }

    private static RequestPostProcessor authenticatedUser(String username) {
        return request -> {
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.emptyList()
                            )
                    );
            return request;
        };
    }
}
