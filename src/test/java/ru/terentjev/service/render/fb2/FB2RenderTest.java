package ru.terentjev.service.render.fb2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.terentjev.service.model.SettingKeys;
import ru.terentjev.service.render.BundledFont;
import ru.terentjev.service.render.Content;
import ru.terentjev.service.render.GoldenImage;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.OpenInfo;
import ru.terentjev.service.render.RenderSettings;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FB2RenderTest {
    private static final String NS = "xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\"" +
            " xmlns:l=\"http://www.w3.org/1999/xlink\"";
    private static final String METADATA = """
            <description><title-info>
              <author><first-name>Isaac</first-name><last-name>Asimov</last-name></author>
              <book-title>David Starr</book-title>
              <sequence name="Lucky Starr" number="1"/>
            </title-info></description>
            """;
    private static final String BODY = "<body><section><p>Text</p></section></body>";
    @TempDir
    Path bookDirectory;
    private final FB2Render render = new FB2Render();

    @Test
    void typeIsFb2() {
        assertEquals("FB2", render.type());
    }

    @Test
    void supportsReaderSettings() {
        assertEquals(
                List.of(
                        SettingKeys.FONT,
                        SettingKeys.SIZE,
                        SettingKeys.COLOR,
                        SettingKeys.BACKGROUND,
                        SettingKeys.PADDING_LEFT,
                        SettingKeys.PADDING_TOP,
                        SettingKeys.PADDING_RIGHT,
                        SettingKeys.PADDING_BOTTOM,
                        SettingKeys.INDENT,
                        SettingKeys.JUSTIFY,
                        SettingKeys.HYPHENATION
                ),
                render.settingsDefinitions()
        );
    }
    // ---------------------------------------------------------------------
    // info
    // ---------------------------------------------------------------------

    @Test
    void infoReadsBookMetadata() throws IOException {
        Info info = render.info(book(METADATA, BODY));
        assertEquals("Isaac Asimov", info.getAuthor());
        assertEquals("David Starr", info.getTitle());
        assertEquals("Lucky Starr", info.getSequenceName());
        assertEquals(1, info.getSequenceNumber());
    }

    @Test
    void infoWithoutSequenceLeavesSequenceEmpty() throws IOException {
        Info info = render.info(
                book(
                        "<description><title-info><book-title>T</book-title></title-info></description>",
                        BODY
                )
        );
        assertNull(info.getAuthor());
        assertEquals("T", info.getTitle());
        assertNull(info.getSequenceName());
        assertNull(info.getSequenceNumber());
    }
    // ---------------------------------------------------------------------
    // open
    // ---------------------------------------------------------------------

    @Test
    void openReturnsPageCountAndOneBasedPositions() throws IOException {
        String body = "<body><section><p>" + String.join(" ", Collections.nCopies(60, "word")) +
                "</p></section></body>";
        OpenInfo info = render.open(
                settings(200, 200),
                book(METADATA, body),
                bookDirectory,
                bookDirectory.resolve("device")
        );
        assertEquals("Isaac Asimov", info.getAuthor());
        assertEquals("David Starr", info.getTitle());
        assertEquals("Lucky Starr", info.getSequenceName());
        assertEquals(1, info.getSequenceNumber());
        assertTrue(info.getPages() > 1, "expected the book to span several pages");
        assertEquals(
                info.getPages(),
                info.getPositionByPage()
                        .size()
        );
        assertEquals(
                IntStream.rangeClosed(1, info.getPages())
                        .boxed()
                        .collect(Collectors.toSet()),
                info.getPositionByPage()
                        .keySet()
        );
        assertTrue(
                info.getPositionByPage()
                        .values()
                        .stream()
                        .allMatch(position -> position >= 0)
        );
    }

    @Test
    void openReturnsHierarchicalContent() throws IOException {
        String body = """
                <body>
                  <section>
                    <title><p>Part One</p></title>
                    <section><title><p>Chapter First</p></title><p>Text</p></section>
                  </section>
                  <section><title><p>Part Two</p></title><p>Text</p></section>
                </body>
                """;
        OpenInfo info = render.open(
                settings(300, 200),
                book(METADATA, body),
                bookDirectory,
                bookDirectory.resolve("device")
        );
        assertEquals(
                2,
                info.getContent()
                        .size()
        );
        Content partOne = info.getContent()
                .get(0);
        assertEquals("Part One", partOne.title());
        assertEquals(
                1,
                partOne.children()
                        .size()
        );
        assertEquals(
                "Chapter First",
                partOne.children()
                        .get(0)
                        .title()
        );
        assertEquals(
                "Part Two",
                info.getContent()
                        .get(1)
                        .title()
        );
        assertTrue(
                info.getContent()
                        .stream()
                        .allMatch(content -> content.position() >= 0)
        );
    }

    @Test
    void openEmptyBodyReturnsNoPages() throws IOException {
        OpenInfo info = render.open(
                settings(300, 200),
                book(METADATA, "<body></body>"),
                bookDirectory,
                bookDirectory.resolve("device")
        );
        assertEquals(0, info.getPages());
        assertTrue(
                info.getPositionByPage()
                        .isEmpty()
        );
    }
    // ---------------------------------------------------------------------
    // cover
    // ---------------------------------------------------------------------

    @Test
    void coverWritesCoverImageBytes() throws IOException {
        byte[] cover = png();
        String metadata = """
                <description><title-info>
                  <book-title>T</book-title>
                  <coverpage><image l:href="#cover.png"/></coverpage>
                </title-info></description>
                """;
        String body = BODY + "<binary id=\"cover.png\" content-type=\"image/png\">" +
                Base64.getEncoder()
                        .encodeToString(cover) +
                "</binary>";
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        render.cover(book(metadata, body), bookDirectory, os);
        assertArrayEquals(cover, os.toByteArray());
    }

    @Test
    void coverWithoutCoverpageWritesNothing() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        render.cover(book(METADATA, BODY), bookDirectory, os);
        assertEquals(0, os.size());
    }

    @Test
    void coverWithMissingBinaryWritesNothing() throws IOException {
        String metadata = """
                <description><title-info>
                  <book-title>T</book-title>
                  <coverpage><image l:href="#missing.png"/></coverpage>
                </title-info></description>
                """;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        render.cover(book(metadata, BODY), bookDirectory, os);
        assertEquals(0, os.size());
    }
    // ---------------------------------------------------------------------
    // render
    // ---------------------------------------------------------------------

    @Test
    void renderScalesViewportByTwo() throws IOException {
        BufferedImage image = renderPage(settings(300, 200), 1);
        assertEquals(600, image.getWidth());
        assertEquals(400, image.getHeight());
    }

    @Test
    void renderUsesDefaultColors() throws IOException {
        BufferedImage image = renderPage(settings(300, 200), 1);
        assertEquals(Color.WHITE.getRGB(), image.getRGB(1, 1));
        assertTrue(containsColor(image, Color.BLACK.getRGB()), "expected black text on the page");
    }

    @Test
    void renderMatchesGolden() throws IOException {
        GoldenImage.assertMatchesGolden("fb2/page-default.png", renderPage(settings(300, 200), 1));
    }

    @Test
    void renderWithConfiguredColorsMatchesGolden() throws IOException {
        RenderSettings settings = settings(
                300,
                200,
                Map.of(
                        SettingKeys.BACKGROUND,
                        "#336699",
                        SettingKeys.COLOR,
                        "#FF0000"
                )
        );
        GoldenImage.assertMatchesGolden("fb2/page-custom-colors.png", renderPage(settings, 1));
    }

    @Test
    void renderStyledPageMatchesGolden() throws IOException {
        String body = """
                <body><section>
                  <title><p>Chapter One</p></title>
                  <subtitle>A subtitle</subtitle>
                  <p>Hello <emphasis>world</emphasis><strong>!</strong></p>
                  <empty-line/>
                  <p>Justified text with several words to wrap the line and check spacing.</p>
                </section></body>
                """;
        GoldenImage.assertMatchesGolden(
                "fb2/page-styled.png",
                renderPage(body, settings(400, 600), 1)
        );
    }

    @Test
    void renderSecondPageMatchesGolden() throws IOException {
        String body = "<body><section><p>" + String.join(" ", Collections.nCopies(60, "word")) +
                "</p></section></body>";
        GoldenImage.assertMatchesGolden(
                "fb2/page-second.png",
                renderPage(body, settings(200, 200), 2)
        );
    }

    @Test
    void renderUsesConfiguredColors() throws IOException {
        RenderSettings settings = settings(
                300,
                200,
                Map.of(
                        SettingKeys.BACKGROUND,
                        "#336699",
                        SettingKeys.COLOR,
                        "#FF0000"
                )
        );
        BufferedImage image = renderPage(settings, 1);
        assertEquals(new Color(0x33, 0x66, 0x99).getRGB(), image.getRGB(1, 1));
        assertTrue(containsColor(image, Color.RED.getRGB()), "expected red text on the page");
    }

    @Test
    void renderFallsBackToDefaultColorsForInvalidValues() throws IOException {
        RenderSettings settings = settings(
                300,
                200,
                Map.of(
                        SettingKeys.BACKGROUND,
                        "not-a-color",
                        SettingKeys.COLOR,
                        "not-a-color"
                )
        );
        BufferedImage image = renderPage(settings, 1);
        assertEquals(Color.WHITE.getRGB(), image.getRGB(1, 1));
        assertTrue(containsColor(image, Color.BLACK.getRGB()), "expected black text on the page");
    }

    @Test
    void renderClampsPageNumberToAvailablePages() throws IOException {
        RenderSettings settings = settings(300, 200);
        byte[] first = renderPng(settings, 1);
        assertArrayEquals(first, renderPng(settings, 0));
        assertArrayEquals(first, renderPng(settings, 99));
    }
    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static RenderSettings settings(int width, int height) {
        return settings(width, height, Map.of());
    }

    private static RenderSettings settings(int width, int height, Map<String, String> values) {
        Map<String, String> all = new HashMap<>(values);
        all.put(SettingKeys.FONT, BundledFont.SANS);
        return new RenderSettings(width, height, all);
    }

    private BufferedImage renderPage(RenderSettings settings, int page) throws IOException {
        return renderPage("<body><section><p>Hello world</p></section></body>", settings, page);
    }

    private BufferedImage renderPage(String body, RenderSettings settings, int page) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(renderPng(body, settings, page)));
    }

    private byte[] renderPng(RenderSettings settings, int page) throws IOException {
        return renderPng("<body><section><p>Hello world</p></section></body>", settings, page);
    }

    private byte[] renderPng(String body, RenderSettings settings, int page) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        render.render(
                settings,
                page,
                book(METADATA, body),
                bookDirectory,
                bookDirectory.resolve("device"),
                os
        );
        return os.toByteArray();
    }

    private static InputStream book(String metadata, String body) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><FictionBook " + NS +
                ">" +
                metadata +
                body +
                "</FictionBook>";
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", os);
        return os.toByteArray();
    }

    private static boolean containsColor(BufferedImage image, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == rgb) {
                    return true;
                }
            }
        }
        return false;
    }
}