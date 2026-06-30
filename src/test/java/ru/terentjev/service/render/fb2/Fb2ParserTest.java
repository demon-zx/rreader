package ru.terentjev.service.render.fb2;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Fb2ParserTest {

    private static final String NS =
            "xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\""
                    + " xmlns:l=\"http://www.w3.org/1999/xlink\"";

    private final Fb2Parser parser = new Fb2Parser();

    @Test
    void readsTitleInfoMetadata() throws IOException {
        Fb2Document document = parse("""
                <description><title-info>
                  <author><first-name>Isaac</first-name><last-name>Asimov</last-name></author>
                  <book-title>David Starr</book-title>
                  <lang>en</lang>
                  <coverpage><image l:href="#cover.jpg"/></coverpage>
                </title-info></description>
                <body><section><p>Text</p></section></body>
                """);

        Fb2Metadata metadata = document.metadata();
        assertEquals("David Starr", metadata.title());
        assertEquals("Isaac Asimov", metadata.author());
        assertEquals("en", metadata.language());
        assertEquals("cover.jpg", metadata.coverHref());
    }

    @Test
    void usesFirstAuthorAndIgnoresTranslator() throws IOException {
        Fb2Document document = parse("""
                <description><title-info>
                  <author><first-name>Isaac</first-name><last-name>Asimov</last-name></author>
                  <author><first-name>Second</first-name><last-name>Writer</last-name></author>
                  <translator><first-name>Trans</first-name><last-name>Lator</last-name></translator>
                  <book-title>T</book-title>
                </title-info></description>
                <body><p>x</p></body>
                """);

        assertEquals("Isaac Asimov", document.metadata().author());
    }

    @Test
    void composesAuthorFromAvailableNameParts() throws IOException {
        Fb2Document document = parse("""
                <description><title-info>
                  <author><last-name>Asimov</last-name></author>
                  <book-title>T</book-title>
                </title-info></description>
                <body><p>x</p></body>
                """);

        assertEquals("Asimov", document.metadata().author());
    }

    @Test
    void missingMetadataFieldsAreNull() throws IOException {
        Fb2Document document = parse("""
                <description><title-info>
                  <book-title>Only title</book-title>
                </title-info></description>
                <body><p>x</p></body>
                """);

        Fb2Metadata metadata = document.metadata();
        assertEquals("Only title", metadata.title());
        assertNull(metadata.author());
        assertNull(metadata.language());
        assertNull(metadata.coverHref());
    }

    @Test
    void splitsParagraphIntoStyledRuns() throws IOException {
        Fb2Document document = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body><section>
                  <p>Hello <emphasis>world</emphasis><strong>!</strong></p>
                </section></body>
                """);

        Fb2Block paragraph = onlyBlock(document);
        assertEquals(Fb2Block.Kind.PARAGRAPH, paragraph.kind());

        List<Fb2Run> runs = paragraph.runs();
        assertEquals(3, runs.size());

        assertEquals("Hello ", runs.get(0).text());
        assertFalse(runs.get(0).bold());
        assertFalse(runs.get(0).italic());

        assertEquals("world", runs.get(1).text());
        assertTrue(runs.get(1).italic());
        assertFalse(runs.get(1).bold());

        assertEquals("!", runs.get(2).text());
        assertTrue(runs.get(2).bold());
        assertFalse(runs.get(2).italic());
    }

    @Test
    void mapsBlockKinds() throws IOException {
        Fb2Document document = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body>
                  <title><p>Chapter One</p></title>
                  <section>
                    <subtitle>A subtitle</subtitle>
                    <p>Body</p>
                    <empty-line/>
                    <image l:href="#img1"/>
                    <section>
                      <title><p>Deep</p></title>
                      <p>Inner</p>
                    </section>
                  </section>
                </body>
                """);

        List<Fb2Block> blocks = document.blocks();
        assertEquals(7, blocks.size());

        assertEquals(Fb2Block.Kind.TITLE, blocks.get(0).kind());
        assertEquals("Chapter One", text(blocks.get(0)));

        assertEquals(Fb2Block.Kind.SUBTITLE, blocks.get(1).kind());
        assertEquals("A subtitle", text(blocks.get(1)));

        assertEquals(Fb2Block.Kind.PARAGRAPH, blocks.get(2).kind());

        assertEquals(Fb2Block.Kind.EMPTY_LINE, blocks.get(3).kind());

        assertEquals(Fb2Block.Kind.IMAGE, blocks.get(4).kind());
        assertEquals("img1", blocks.get(4).imageHref());

        assertEquals(Fb2Block.Kind.SUBTITLE, blocks.get(5).kind());
        assertEquals("Deep", text(blocks.get(5)));

        assertEquals(Fb2Block.Kind.PARAGRAPH, blocks.get(6).kind());
        assertEquals("Inner", text(blocks.get(6)));
    }

    @Test
    void parsesOnlyTheFirstBody() throws IOException {
        Fb2Document document = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body><p>Main</p></body>
                <body name="notes"><p>Footnote</p></body>
                """);

        Fb2Block paragraph = onlyBlock(document);
        assertEquals("Main", text(paragraph));
    }

    @Test
    void decodesBinariesByIdIncludingWhitespace() throws IOException {
        Fb2Document document = parse("""
                <description><title-info>
                  <book-title>T</book-title>
                  <coverpage><image l:href="#cover.jpg"/></coverpage>
                </title-info></description>
                <body><p>x</p></body>
                <binary id="cover.jpg" content-type="image/jpeg">aGVs
                bG8=</binary>
                """);

        String coverId = document.metadata().coverHref();
        assertEquals("cover.jpg", coverId);
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), document.binary(coverId));
        assertNull(document.binary("missing"));
    }

    @Test
    void handlesDocumentWithoutBody() throws IOException {
        Fb2Document document = parse("""
                <description><title-info><book-title>Only meta</book-title></title-info></description>
                """);

        assertEquals("Only meta", document.metadata().title());
        assertTrue(document.blocks().isEmpty());
    }

    @Test
    void honoursXmlDeclaredEncoding() throws IOException {
        String xml = wrap("""
                <description><title-info><book-title>Привет</book-title></title-info></description>
                <body><p>текст</p></body>
                """, "windows-1251");

        Fb2Document document = parser.parse(
                new ByteArrayInputStream(xml.getBytes(Charset.forName("windows-1251"))));

        assertEquals("Привет", document.metadata().title());
        assertEquals("текст", text(onlyBlock(document)));
    }

    private Fb2Document parse(String descriptionAndBody) throws IOException {
        String xml = wrap(descriptionAndBody, "UTF-8");
        return parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String wrap(String body, String encoding) {
        return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n"
                + "<FictionBook " + NS + ">" + body + "</FictionBook>";
    }

    private static Fb2Block onlyBlock(Fb2Document document) {
        List<Fb2Block> blocks = document.blocks();
        assertEquals(1, blocks.size(), "expected exactly one block");
        return blocks.get(0);
    }

    private static String text(Fb2Block block) {
        StringBuilder sb = new StringBuilder();
        for (Fb2Run run : block.runs()) {
            sb.append(run.text());
        }
        return sb.toString();
    }
}
