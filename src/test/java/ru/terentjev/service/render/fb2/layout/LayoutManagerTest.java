package ru.terentjev.service.render.fb2.layout;

import org.junit.jupiter.api.Test;

import ru.terentjev.service.render.fb2.layout.Page.ImageObject;
import ru.terentjev.service.render.fb2.layout.Page.LineObject;
import ru.terentjev.service.render.fb2.layout.Page.PageObject;
import ru.terentjev.service.render.fb2.layout.Page.Word;
import ru.terentjev.service.render.fb2.parser.FB2;
import ru.terentjev.service.render.fb2.parser.FB2.Block;
import ru.terentjev.service.render.fb2.parser.FB2.EmptyLineBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ImageBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ParagraphBlock;
import ru.terentjev.service.render.fb2.parser.FB2.Piece;
import ru.terentjev.service.render.fb2.parser.FB2.SubTitleBlock;
import ru.terentjev.service.render.fb2.parser.FB2.TitleBlock;
import ru.terentjev.service.render.fb2.parser.FB2RenderSettings;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutManagerTest {
    private static final int FONT_SIZE = 20;
    /** Wide and tall enough that short content never wraps or breaks pages on its own. */
    private static final FB2RenderSettings WIDE = settings(5000, 10000);
    // ---------------------------------------------------------------------
    // page flushing
    // ---------------------------------------------------------------------

    @Test
    void emptyBodyProducesNoPages() throws IOException {
        assertEquals(0, layout(doc(Map.of()), WIDE).size());
    }

    @Test
    void trailingParagraphIsFlushedIntoFinalPage() throws IOException {
        FB2 fb2 = doc(Map.of(), new ParagraphBlock(7, List.of(piece("Hello world"))));
        List<Page> pages = layout(fb2, WIDE);
        assertEquals(1, pages.size());
        Page page = pages.get(0);
        assertEquals(7, page.position());
        assertEquals(
                1,
                page.objects()
                        .size()
        );
        assertEquals(List.of("Hello", "world"), values(page));
    }

    @Test
    void emptyLineAloneProducesNoPages() throws IOException {
        // An empty line only advances the vertical cursor; it adds no page object,
        // so there is nothing to flush.
        assertEquals(0, layout(doc(Map.of(), new EmptyLineBlock(0)), WIDE).size());
    }

    @Test
    void firstParagraphLineIsIndentedByIndentSetting() throws IOException {
        FB2RenderSettings settings = new FB2RenderSettings(
                400,
                10000,
                20,
                0,
                20,
                0,
                40,
                "Arial",
                FONT_SIZE,
                1.2f,
                Color.BLACK,
                Color.WHITE
        );
        FB2 fb2 = doc(
                Map.of(),
                new ParagraphBlock(
                        1,
                        List.of(
                                piece("aa bb cc dd ee ff gg hh ii jj kk ll mm nn oo pp")
                        )
                )
        );
        List<Page> pages = layout(fb2, settings);
        assertEquals(1, pages.size());
        List<LineObject> lines = pages.get(0)
                .objects()
                .stream()
                .filter(o -> o instanceof LineObject)
                .map(o -> (LineObject) o)
                .toList();
        assertTrue(lines.size() > 1, "expected the paragraph to wrap onto multiple lines");
        assertEquals(
                20 + 40,
                lines.get(0)
                        .words()
                        .get(0)
                        .x()
        );
        assertEquals(
                20,
                lines.get(1)
                        .words()
                        .get(0)
                        .x()
        );
    }

    @Test
    void emptyLineAddsNoObjectToThePage() throws IOException {
        FB2 fb2 = doc(Map.of(), new ParagraphBlock(0, List.of(piece("text"))), new EmptyLineBlock(1));
        List<Page> pages = layout(fb2, WIDE);
        assertEquals(1, pages.size());
        assertEquals(
                1,
                pages.get(0)
                        .objects()
                        .size()
        );
        assertEquals(List.of("text"), values(pages.get(0)));
    }
    // ---------------------------------------------------------------------
    // images
    // ---------------------------------------------------------------------

    @Test
    void resolvableImageProducesOwnPage() throws IOException {
        FB2 fb2 = doc(Map.of("img1", "/path/to/img.png"), new ImageBlock(42, "img1"));
        List<Page> pages = layout(fb2, WIDE);
        assertEquals(1, pages.size());
        Page page = pages.get(0);
        assertEquals(42, page.position());
        assertEquals(
                1,
                page.objects()
                        .size()
        );
        ImageObject image = assertInstanceOf(
                ImageObject.class,
                page.objects()
                        .get(0)
        );
        assertEquals("/path/to/img.png", image.href());
    }

    @Test
    void unresolvableImageProducesNoPages() throws IOException {
        FB2 fb2 = doc(Map.of(), new ImageBlock(0, "missing"));
        assertEquals(0, layout(fb2, WIDE).size());
    }

    @Test
    void paragraphIsFlushedOntoSeparatePageBeforeImage() throws IOException {
        FB2 fb2 = doc(
                Map.of("i", "/p.png"),
                new ParagraphBlock(100, List.of(piece("Hello world"))),
                new ImageBlock(200, "i")
        );
        List<Page> pages = layout(fb2, WIDE);
        assertEquals(2, pages.size());
        Page textPage = pages.get(0);
        assertEquals(100, textPage.position());
        assertEquals(List.of("Hello", "world"), values(textPage));
        Page imagePage = pages.get(1);
        assertEquals(200, imagePage.position());
        assertEquals(
                1,
                imagePage.objects()
                        .size()
        );
        assertInstanceOf(
                ImageObject.class,
                imagePage.objects()
                        .get(0)
        );
    }
    // ---------------------------------------------------------------------
    // tokenization & fonts
    // ---------------------------------------------------------------------

    @Test
    void selectsFontPerPieceStyle() throws IOException {
        FB2 fb2 = doc(
                Map.of(),
                new ParagraphBlock(
                        0,
                        List.of(
                                new Piece("plain", false, false, null),
                                new Piece("bold", true, false, null),
                                new Piece("italic", false, true, null),
                                new Piece("both", true, true, null)
                        )
                )
        );
        List<Word> words = words(layout(fb2, WIDE).get(0));
        assertEquals(
                List.of("plain", "bold", "italic", "both"),
                words.stream()
                        .map(Word::value)
                        .toList()
        );
        Font plain = fontOf(words, "plain");
        assertFalse(plain.isBold());
        assertFalse(plain.isItalic());
        Font bold = fontOf(words, "bold");
        assertTrue(bold.isBold());
        assertFalse(bold.isItalic());
        Font italic = fontOf(words, "italic");
        assertFalse(italic.isBold());
        assertTrue(italic.isItalic());
        Font both = fontOf(words, "both");
        assertTrue(both.isBold());
        assertTrue(both.isItalic());
    }

    @Test
    void titleUsesBoldDoubleSizeFont() throws IOException {
        FB2 fb2 = doc(Map.of(), new TitleBlock(0, List.of(piece("Chapter"))));
        Font font = words(layout(fb2, WIDE).get(0)).get(0)
                .font();
        assertTrue(font.isBold());
        assertEquals(FONT_SIZE * 2, font.getSize());
    }

    @Test
    void subTitleUsesBoldOneAndHalfSizeFont() throws IOException {
        FB2 fb2 = doc(Map.of(), new SubTitleBlock(0, List.of(piece("Section"))));
        Font font = words(layout(fb2, WIDE).get(0)).get(0)
                .font();
        assertTrue(font.isBold());
        assertEquals((int) (FONT_SIZE * 1.5), font.getSize());
    }

    @Test
    void collapsesWhitespaceIntoSeparateWords() throws IOException {
        FB2 fb2 = doc(
                Map.of(),
                new ParagraphBlock(0, List.of(piece("  one   two\tthree \n four  ")))
        );
        assertEquals(List.of("one", "two", "three", "four"), values(layout(fb2, WIDE).get(0)));
    }
    // ---------------------------------------------------------------------
    // word wrapping
    // ---------------------------------------------------------------------

    @Test
    void wrapsLongParagraphAcrossMultipleLinesPreservingWordOrder() throws IOException {
        List<String> expected = List.of("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");
        FB2 fb2 = doc(Map.of(), new ParagraphBlock(0, List.of(piece(String.join(" ", expected)))));
        // Narrow width forces wrapping; tall height keeps every line on the first page.
        Page page = layout(fb2, settings(120, 10000)).get(0);
        long lines = page.objects()
                .stream()
                .filter(o -> o instanceof LineObject)
                .count();
        assertTrue(lines > 1, "expected the paragraph to wrap onto multiple lines");
        assertEquals(expected, values(page));
    }
    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private List<Page> layout(FB2 document, FB2RenderSettings settings) throws IOException {
        return new LayoutManager(settings).layout(document);
    }

    private static FB2RenderSettings settings(int width, int height) {
        return new FB2RenderSettings(
                width,
                height,
                0,
                0,
                0,
                0,
                0,
                "Arial",
                FONT_SIZE,
                1.2f,
                Color.BLACK,
                Color.WHITE
        );
    }

    private static FB2 doc(Map<String, String> binaries, Block... blocks) {
        return new FB2(
                "title",
                List.of(),
                List.of(),
                null,
                null,
                null,
                List.of(),
                List.of(blocks),
                Map.of(),
                binaries
        );
    }

    private static Piece piece(String text) {
        return new Piece(text, false, false, null);
    }

    private static List<Word> words(Page page) {
        List<Word> words = new ArrayList<>();
        for (PageObject object : page.objects()) {
            if (object instanceof LineObject line) {
                words.addAll(line.words());
            }
        }
        return words;
    }

    private static List<String> values(Page page) {
        return words(page).stream()
                .map(Word::value)
                .toList();
    }

    private static Font fontOf(List<Word> words, String value) {
        return words.stream()
                .filter(
                        w -> w.value()
                                .equals(value)
                )
                .map(Word::font)
                .findFirst()
                .orElseThrow();
    }
}
