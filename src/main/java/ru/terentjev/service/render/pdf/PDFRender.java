package ru.terentjev.service.render.pdf;

import org.apache.commons.lang3.function.FailableBiConsumer;
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
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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
        return new Info(pages, author, title);
    }

    @Override
    public void render(
            InputStream is,
            FailableBiConsumer<RenderedImage, Integer, IOException> renderedPageConsumer,
            List<Integer> pages
    ) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (Integer page : pages) {
            log.info("Rendering page {}", page);
            BufferedImage image = pdfRenderer.renderImageWithDPI(page - 1, 150);
            renderedPageConsumer.accept(image, page);
        }
    }

    @Override
    public void render(
            int page,
            InputStream is,
            OutputStream os
    ) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        log.info("Rendering page {}", page);
        BufferedImage image = pdfRenderer.renderImageWithDPI(page - 1, 150);
        ImageIO.write(image, "PNG", os);
        os.flush();
    }

}
