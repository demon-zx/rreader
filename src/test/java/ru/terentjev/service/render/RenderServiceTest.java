package ru.terentjev.service.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.terentjev.config.DirectoryParameters;
import ru.terentjev.service.BookService;
import ru.terentjev.service.SettingsService;
import ru.terentjev.service.model.Book;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.Settings;
import ru.terentjev.service.model.SettingsScope;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderServiceTest {

    private static final UUID DEVICE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final UUID bookId = UUID.randomUUID();

    @TempDir
    Path tempDir;

    @Mock
    private BookService service;

    @Mock
    private RenderFactory renderFactory;

    @Mock
    private SettingsService settingsService;

    @Mock
    private Render render;

    private RenderService renderService;

    @BeforeEach
    void setUp() {
        renderService = new RenderService(
                service,
                renderFactory,
                settingsService,
                new DirectoryParameters(
                        tempDir.resolve("config"),
                        tempDir.resolve("books"),
                        tempDir.resolve("pages")
                )
        );
    }

    @Test
    void openReadsEffectiveSettingsForBookScope() throws Exception {
        Book book = book("FB2");
        when(service.findById(bookId)).thenReturn(book);
        when(renderFactory.getRender("FB2")).thenReturn(render);
        when(settingsService.read(DEVICE, SettingsScope.FB2)).thenReturn(settings(
                new KeyValue("font", "Georgia"),
                new KeyValue("size", "30")
        ));

        renderService.open(bookId, DEVICE, 1080, 1527);

        ArgumentCaptor<RenderSettings> captor = ArgumentCaptor.forClass(RenderSettings.class);
        verify(render).open(captor.capture(), any(InputStream.class), any(Path.class), any(Path.class));
        RenderSettings settings = captor.getValue();
        assertEquals(1080, settings.viewportWidth());
        assertEquals(1527, settings.viewportHeight());
        assertEquals("Georgia", settings.value("font", null));
        assertEquals(30, settings.intValue("size", 0));
    }

    @Test
    void renderPagePassesEffectiveSettings() throws Exception {
        Book book = book("FB2");
        when(service.findById(bookId)).thenReturn(book);
        when(renderFactory.getRender("FB2")).thenReturn(render);
        when(settingsService.read(DEVICE, SettingsScope.FB2)).thenReturn(settings(
                new KeyValue("color", "#123456"),
                new KeyValue("background", "#654321"),
                new KeyValue("paddingLeft", "40"),
                new KeyValue("paddingTop", "30"),
                new KeyValue("paddingRight", "20"),
                new KeyValue("paddingBottom", "10")
        ));

        renderService.renderPage(bookId, 1, DEVICE, 1080, 1527, is -> {});

        ArgumentCaptor<RenderSettings> captor = ArgumentCaptor.forClass(RenderSettings.class);
        verify(render).render(
                captor.capture(),
                org.mockito.ArgumentMatchers.eq(1),
                any(InputStream.class),
                any(Path.class),
                any(Path.class),
                any(OutputStream.class)
        );
        RenderSettings settings = captor.getValue();
        assertEquals("#123456", settings.value("color", null));
        assertEquals("#654321", settings.value("background", null));
        assertEquals(40, settings.intValue("paddingLeft", 0));
        assertEquals(30, settings.intValue("paddingTop", 0));
        assertEquals(20, settings.intValue("paddingRight", 0));
        assertEquals(10, settings.intValue("paddingBottom", 0));
    }

    @Test
    void unknownBookTypeFallsBackToGlobalScope() throws Exception {
        Book book = book("EPUB");
        when(service.findById(bookId)).thenReturn(book);
        when(renderFactory.getRender("EPUB")).thenReturn(render);
        when(settingsService.read(DEVICE, SettingsScope.GLOBAL)).thenReturn(settings());

        renderService.open(bookId, DEVICE, 1080, 1527);

        verify(settingsService).read(DEVICE, SettingsScope.GLOBAL);
    }

    private Book book(String type) throws Exception {
        Path file = Files.createFile(tempDir.resolve("book." + type.toLowerCase()));
        return new Book(bookId, "Title", null, null, null, null, type, file.toUri(), "checksum", null, 100);
    }

    private static Settings settings(KeyValue... values) {
        return new Settings("FB2", List.of(values));
    }
}
