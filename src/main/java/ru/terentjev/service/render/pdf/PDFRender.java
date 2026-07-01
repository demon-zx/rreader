package ru.terentjev.service.render.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.Render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component
public class PDFRender implements Render {

    private static final Logger log = LoggerFactory.getLogger(PDFRender.class);

    @Override
    public String type() {
        return "PDF";
    }

    @Override
    public Info info(InputStream is) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        int pages = document.getNumberOfPages();
        PDDocumentInformation information = document.getDocumentInformation();
        String author = information.getAuthor();
        String title = information.getTitle();
        return new Info(pages, author, title, null, null);
    }

    @Override
    public void cover(
            InputStream is,
            OutputStream os
    ) throws IOException {
        render(1, is, os, 75);
    }

    @Override
    public void render(
            int page,
            InputStream is,
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
