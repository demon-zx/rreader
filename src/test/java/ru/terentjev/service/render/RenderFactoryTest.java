package ru.terentjev.service.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderFactoryTest {
    @Test
    void resolvesRenderByType() {
        Render fb2 = new StubRender("FB2", List.of("font"));
        Render pdf = new StubRender("PDF", List.of());
        RenderFactory factory = new RenderFactory(List.of(fb2, pdf));
        assertSame(fb2, factory.getRender("FB2"));
        assertSame(pdf, factory.getRender("PDF"));
    }

    @Test
    void resolvesRenderIgnoringCase() {
        Render fb2 = new StubRender("FB2", List.of());
        RenderFactory factory = new RenderFactory(List.of(fb2));
        assertSame(fb2, factory.getRender("fb2"));
        assertSame(fb2, factory.getRender("Fb2"));
    }

    @Test
    void unknownTypeThrowsException() {
        RenderFactory factory = new RenderFactory(List.of(new StubRender("FB2", List.of())));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getRender("EPUB")
        );
        assertEquals("Unknown render type: EPUB", exception.getMessage());
    }

    @Test
    void lastRenderWinsForDuplicateType() {
        Render first = new StubRender("FB2", List.of());
        Render second = new StubRender("FB2", List.of());
        RenderFactory factory = new RenderFactory(List.of(first, second));
        assertSame(second, factory.getRender("FB2"));
    }

    @Test
    void settingsDefinitionsForKnownType() {
        Render fb2 = new StubRender("FB2", List.of("font", "size"));
        RenderFactory factory = new RenderFactory(List.of(fb2));
        assertEquals(List.of("font", "size"), factory.settingsDefinitions("FB2"));
    }

    @Test
    void settingsDefinitionsForUnknownTypeIsEmpty() {
        RenderFactory factory = new RenderFactory(List.of(new StubRender("FB2", List.of("font"))));
        assertTrue(
                factory.settingsDefinitions("EPUB")
                        .isEmpty()
        );
    }

    private static class StubRender implements Render {
        private final String type;
        private final List<String> definitions;

        private StubRender(String type, List<String> definitions) {
            this.type = type;
            this.definitions = definitions;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public List<String> settingsDefinitions() {
            return definitions;
        }

        @Override
        public Info info(InputStream is) {
            return null;
        }

        @Override
        public OpenInfo open(
                RenderSettings settings,
                InputStream is,
                Path bookDirectory,
                Path deviceDirectory
        ) {
            return null;
        }

        @Override
        public void cover(InputStream is, Path bookDirectory, OutputStream os) throws IOException {
        }

        @Override
        public void render(
                RenderSettings settings,
                int page,
                InputStream is,
                Path bookDirectory,
                Path deviceDirectory,
                OutputStream os
        ) throws IOException {
        }
    }
}