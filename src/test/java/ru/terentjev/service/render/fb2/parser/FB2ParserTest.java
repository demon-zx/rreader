package ru.terentjev.service.render.fb2.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.terentjev.service.render.fb2.parser.FB2.AbstractPiecesBlock;
import ru.terentjev.service.render.fb2.parser.FB2.Block;
import ru.terentjev.service.render.fb2.parser.FB2.EmptyLineBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ImageBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ParagraphBlock;
import ru.terentjev.service.render.fb2.parser.FB2.Piece;
import ru.terentjev.service.render.fb2.parser.FB2.Sequence;
import ru.terentjev.service.render.fb2.parser.FB2.SubTitleBlock;
import ru.terentjev.service.render.fb2.parser.FB2.TitleBlock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FB2ParserTest {
    private static final String NS = "xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\"" + " xmlns:l=\"http://www.w3.org/1999/xlink\"";
    @TempDir
    Path workDirectory;
    // ---------------------------------------------------------------------
    // metadata
    // ---------------------------------------------------------------------

    @Test
    void readsTitleInfoMetadata() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <genre>sf</genre>
                  <author><first-name>Isaac</first-name><last-name>Asimov</last-name></author>
                  <book-title>David Starr</book-title>
                  <lang>en</lang>
                  <date>1952-01-15</date>
                  <coverpage><image l:href="#cover.jpg"/></coverpage>
                </title-info></description>
                <body><section><p>Text</p></section></body>
                """);
        assertEquals("David Starr", fb2.title());
        assertEquals(List.of("Isaac Asimov"), fb2.authors());
        assertEquals(List.of("sf"), fb2.genres());
        assertEquals("en", fb2.lang());
        assertEquals(LocalDate.of(1952, 1, 15), fb2.date());
        assertEquals("cover.jpg", fb2.cover());
    }

    @Test
    void collectsAllAuthorsIgnoringTranslator() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <author><first-name>Isaac</first-name><last-name>Asimov</last-name></author>
                  <author><first-name>Second</first-name><last-name>Writer</last-name></author>
                  <translator><first-name>Trans</first-name><last-name>Lator</last-name></translator>
                  <book-title>T</book-title>
                </title-info></description>
                <body><p>x</p></body>
                """);
        assertEquals(List.of("Isaac Asimov", "Second Writer"), fb2.authors());
    }

    @Test
    void composesAuthorFromAvailableNameParts() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <author><last-name>Asimov</last-name></author>
                  <book-title>T</book-title>
                </title-info></description>
                <body><p>x</p></body>
                """);
        assertEquals(List.of("Asimov"), fb2.authors());
    }

    @Test
    void missingOptionalMetadataFieldsAreNullOrEmpty() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <book-title>Only title</book-title>
                </title-info></description>
                <body><p>x</p></body>
                """);
        assertEquals("Only title", fb2.title());
        assertTrue(
                fb2.authors()
                        .isEmpty()
        );
        assertTrue(
                fb2.genres()
                        .isEmpty()
        );
        assertTrue(
                fb2.sequences()
                        .isEmpty()
        );
        assertNull(fb2.lang());
        assertNull(fb2.date());
        assertNull(fb2.cover());
    }

    @Test
    void ignoresInvalidDate() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <book-title>T</book-title>
                  <date>not-a-date</date>
                </title-info></description>
                <body><p>x</p></body>
                """);
        assertNull(fb2.date());
    }
    // ---------------------------------------------------------------------
    // sequences
    // ---------------------------------------------------------------------

    @Test
    void readsSequenceNameAndNumber() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <book-title>David Starr</book-title>
                  <sequence name="Lucky Starr" number="1"/>
                </title-info></description>
                <body><section><p>Text</p></section></body>
                """);
        assertEquals(List.of(new Sequence("Lucky Starr", 1)), fb2.sequences());
    }

    @Test
    void sequenceWithoutNumberIsSkipped() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <book-title>T</book-title>
                  <sequence name="Series"/>
                </title-info></description>
                <body><p>x</p></body>
                """);
        assertTrue(
                fb2.sequences()
                        .isEmpty()
        );
    }

    @Test
    void sequenceWithNonNumericNumberIsSkipped() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <book-title>T</book-title>
                  <sequence name="Series" number="abc"/>
                </title-info></description>
                <body><p>x</p></body>
                """);
        assertEquals(
                0,
                fb2.sequences()
                        .size()
        );
    }
    // ---------------------------------------------------------------------
    // body blocks
    // ---------------------------------------------------------------------

    @Test
    void mapsBlockKinds() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body>
                  <section>
                    <title><p>Chapter One</p></title>
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
        List<Block> blocks = fb2.body();
        assertEquals(7, blocks.size());
        assertInstanceOf(TitleBlock.class, blocks.get(0));
        assertEquals("Chapter One", text(blocks.get(0)));
        assertInstanceOf(SubTitleBlock.class, blocks.get(1));
        assertEquals("A subtitle", text(blocks.get(1)));
        assertInstanceOf(ParagraphBlock.class, blocks.get(2));
        assertEquals("Body", text(blocks.get(2)));
        assertInstanceOf(EmptyLineBlock.class, blocks.get(3));
        assertInstanceOf(ImageBlock.class, blocks.get(4));
        assertEquals("img1", ((ImageBlock) blocks.get(4)).href());
        assertInstanceOf(SubTitleBlock.class, blocks.get(5));
        assertEquals("Deep", text(blocks.get(5)));
        assertInstanceOf(ParagraphBlock.class, blocks.get(6));
        assertEquals("Inner", text(blocks.get(6)));
    }

    @Test
    void topLevelTitleIsTitleKind() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body>
                  <title><p>Book title</p></title>
                  <section><p>Body</p></section>
                </body>
                """);
        List<Block> blocks = fb2.body();
        assertInstanceOf(TitleBlock.class, blocks.get(0));
        assertEquals("Book title", text(blocks.get(0)));
    }

    @Test
    void parsesOnlyTheFirstUnnamedBodyForBlocks() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body><section><p>Main</p></section></body>
                """);
        assertEquals(
                1,
                fb2.body()
                        .size()
        );
        assertEquals(
                "Main",
                text(
                        fb2.body()
                                .get(0)
                )
        );
    }

    @Test
    void splitsParagraphIntoStyledRuns() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body><section>
                  <p>Hello <emphasis>world</emphasis><strong>!</strong></p>
                </section></body>
                """);
        ParagraphBlock paragraph = assertInstanceOf(ParagraphBlock.class, onlyBlock(fb2));
        List<Piece> pieces = paragraph.pieces();
        assertEquals(3, pieces.size());
        assertEquals(
                "Hello",
                pieces.get(0)
                        .text()
        );
        assertFalse(
                pieces.get(0)
                        .bold()
        );
        assertFalse(
                pieces.get(0)
                        .italic()
        );
        assertEquals(
                "world",
                pieces.get(1)
                        .text()
        );
        assertTrue(
                pieces.get(1)
                        .italic()
        );
        assertFalse(
                pieces.get(1)
                        .bold()
        );
        assertEquals(
                "!",
                pieces.get(2)
                        .text()
        );
        assertTrue(
                pieces.get(2)
                        .bold()
        );
        assertFalse(
                pieces.get(2)
                        .italic()
        );
    }

    @Test
    void marksNoteLinkPieceWithHref() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body><section>
                  <p>See <a l:href="#n1" type="note">1</a> here</p>
                </section></body>
                """);
        ParagraphBlock paragraph = assertInstanceOf(ParagraphBlock.class, onlyBlock(fb2));
        List<Piece> pieces = paragraph.pieces();
        assertEquals(3, pieces.size());
        assertEquals(
                "See",
                pieces.get(0)
                        .text()
        );
        assertNull(
                pieces.get(0)
                        .linkId()
        );
        assertEquals(
                "1",
                pieces.get(1)
                        .text()
        );
        assertEquals(
                "#n1",
                pieces.get(1)
                        .linkId()
        );
        assertEquals(
                "here",
                pieces.get(2)
                        .text()
        );
        assertNull(
                pieces.get(2)
                        .linkId()
        );
    }
    // ---------------------------------------------------------------------
    // notes body
    // ---------------------------------------------------------------------

    @Test
    void collectsNotesBySectionId() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info><book-title>T</book-title></title-info></description>
                <body><section><p>Main</p></section></body>
                <body name="notes">
                  <section id="n1"><p>First note</p></section>
                  <section id="n2"><p>Second note</p></section>
                </body>
                """);
        assertEquals(
                List.of("n1", "n2"),
                fb2.notes()
                        .keySet()
                        .stream()
                        .sorted()
                        .toList()
        );
        assertEquals(
                "First note",
                text(
                        fb2.notes()
                                .get("n1")
                                .get(0)
                )
        );
        assertEquals(
                "Second note",
                text(
                        fb2.notes()
                                .get("n2")
                                .get(0)
                )
        );
    }
    // ---------------------------------------------------------------------
    // binaries
    // ---------------------------------------------------------------------

    @Test
    void decodesBinaryContentToFile() throws IOException {
        FB2 fb2 = parse("""
                <description><title-info>
                  <book-title>T</book-title>
                  <coverpage><image l:href="#cover.jpg"/></coverpage>
                </title-info></description>
                <body><p>x</p></body>
                <binary id="cover.jpg" content-type="image/jpeg">aGVs
                bG8=</binary>
                """);
        String binaryPath = fb2.binaries()
                .get("cover.jpg");
        Path file = Path.of(binaryPath);
        assertTrue(Files.exists(file), "binary should be written to disk");
        assertTrue(file.startsWith(workDirectory.resolve("binaries")));
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(file));
    }
    // ---------------------------------------------------------------------
    // encoding
    // ---------------------------------------------------------------------

    @Test
    void honoursXmlDeclaredEncoding() throws IOException {
        String xml = wrap("""
                <description><title-info><book-title>Привет</book-title></title-info></description>
                <body><section><p>текст</p></section></body>
                """, "windows-1251");
        FB2 fb2 = new FB2Parser(workDirectory)
                .parse(new ByteArrayInputStream(xml.getBytes(Charset.forName("windows-1251"))));
        assertEquals("Привет", fb2.title());
        assertEquals("текст", text(onlyBlock(fb2)));
    }
    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private FB2 parse(String descriptionAndBody) throws IOException {
        String xml = wrap(descriptionAndBody, "UTF-8");
        return new FB2Parser(workDirectory)
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String wrap(String body, String encoding) {
        return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" + "<FictionBook " + NS + ">" + body + "</FictionBook>";
    }

    private static Block onlyBlock(FB2 fb2) {
        List<Block> blocks = fb2.body();
        assertEquals(1, blocks.size(), "expected exactly one block");
        return blocks.get(0);
    }

    private static String text(Block block) {
        StringBuilder sb = new StringBuilder();
        AbstractPiecesBlock piecesBlock = assertInstanceOf(AbstractPiecesBlock.class, block);
        for (Piece piece : piecesBlock.pieces()) {
            sb.append(piece.text());
        }
        return sb.toString();
    }
}
