package ru.terentjev.service.render.pdf;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ru.terentjev.service.render.Content;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.OpenInfo;
import ru.terentjev.service.render.Render;
import ru.terentjev.service.render.RenderSettings;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

@Component
public class PDFRender implements Render {
    private static final Logger log = LoggerFactory.getLogger(PDFRender.class);

    @Override
    public String type() {
        return "PDF";
    }

    @Override
    public List<String> settingsDefinitions() {
        return List.of();
    }

    @Override
    public Info info(InputStream is) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        PDDocumentInformation information = document.getDocumentInformation();
        String author = information.getAuthor();
        String title = information.getTitle();
        return new Info(author, title, null, null);
    }

    @Override
    public OpenInfo open(
            RenderSettings settings,
            InputStream is,
            Path bookDirectory,
            Path deviceDirectory
    ) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        int pages = document.getNumberOfPages();
        PDDocumentInformation information = document.getDocumentInformation();
        String author = information.getAuthor();
        String title = information.getTitle();
        return new OpenInfo(author, title, pages, null, null, Map.of(), content(document));
    }

    /**
     * Строит содержание по закладкам pdf. Если закладок нет, содержание пустое.
     *
     * @param document документ pdf
     * @return иерархическое содержание
     */
    private static List<Content> content(PDDocument document) throws IOException {
        PDDocumentOutline outline = document.getDocumentCatalog()
                .getDocumentOutline();
        if (outline == null) {
            return List.of();
        }
        return content(document, outline.getFirstChild());
    }

    private static List<Content> content(PDDocument document, PDOutlineItem first) throws IOException {
        List<Content> result = new ArrayList<>();
        for (PDOutlineItem item = first; item != null; item = item.getNextSibling()) {
            String title = StringUtils.normalizeSpace(StringUtils.defaultString(item.getTitle()));
            PDPage page = item.findDestinationPage(document);
            int position = page == null ? 1 : document.getPages()
                    .indexOf(page) + 1;
            result.add(new Content(title, position, content(document, item.getFirstChild())));
        }
        return result;
    }

    @Override
    public void cover(InputStream is, Path bookDirectory, OutputStream os) throws IOException {
        render(1, is, os, 75);
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
        render(page, is, os, 300);
    }

    private static void render(int page, InputStream is, OutputStream os, int dpi) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        log.info("Rendering page {}", page);
        BufferedImage image = pdfRenderer.renderImageWithDPI(page - 1, dpi);
        ImageIO.write(image, "PNG", os);
        os.flush();
    }
}
