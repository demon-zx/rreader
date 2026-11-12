package ru.terentjev.service.render.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.terentjev.service.render.GoldenImage;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.OpenInfo;
import ru.terentjev.service.render.RenderSettings;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFRenderTest {
    private static final String AUTHOR = "Isaac Asimov";
    private static final String TITLE = "David Starr";
    @TempDir
    Path bookDirectory;
    private final PDFRender render = new PDFRender();

    @Test
    void typeIsPdf() {
        assertEquals("PDF", render.type());
    }

    @Test
    void supportsNoSettings() {
        assertTrue(
                render.settingsDefinitions()
                        .isEmpty()
        );
    }

    @Test
    void infoReadsDocumentMetadata() throws IOException {
        Info info = render.info(pdf(1));
        assertEquals(AUTHOR, info.getAuthor());
        assertEquals(TITLE, info.getTitle());
    }

    @Test
    void openReturnsPageCountAndMetadata() throws IOException {
        OpenInfo info = render.open(
                new RenderSettings(1080, 1527),
                pdf(3),
                bookDirectory,
                bookDirectory.resolve("device")
        );
        assertEquals(AUTHOR, info.getAuthor());
        assertEquals(TITLE, info.getTitle());
        assertEquals(3, info.getPages());
        assertTrue(
                info.getPositionByPage()
                        .isEmpty()
        );
    }

    @Test
    void coverMatchesGolden() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        render.cover(pdf(2), bookDirectory, os);
        GoldenImage.assertMatchesGolden("pdf/cover.png", image(os.toByteArray()));
    }

    @Test
    void renderFirstPageMatchesGolden() throws IOException {
        GoldenImage.assertMatchesGolden("pdf/page-1.png", image(renderPng(1)));
    }

    @Test
    void renderSecondPageMatchesGolden() throws IOException {
        GoldenImage.assertMatchesGolden("pdf/page-2.png", image(renderPng(2)));
    }

    @Test
    void renderOfDifferentPagesProducesDifferentImages() throws IOException {
        BufferedImage first = image(renderPng(1));
        BufferedImage second = image(renderPng(2));
        assertFalse(samePixels(first, second));
    }
    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private byte[] renderPng(int page) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        render.render(
                new RenderSettings(1080, 1527),
                page,
                pdf(2),
                bookDirectory,
                bookDirectory.resolve("device"),
                os
        );
        return os.toByteArray();
    }

    private static BufferedImage image(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    private static InputStream pdf(int pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 1; i <= pages; i++) {
                PDPage page = new PDPage(PDRectangle.A6);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(20, 100 + i * 20);
                    content.showText("Page " + i);
                    content.endText();
                }
            }
            PDDocumentInformation information = new PDDocumentInformation();
            information.setAuthor(AUTHOR);
            information.setTitle(TITLE);
            document.setDocumentInformation(information);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            document.save(os);
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private static boolean samePixels(BufferedImage first, BufferedImage second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return false;
        }
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}