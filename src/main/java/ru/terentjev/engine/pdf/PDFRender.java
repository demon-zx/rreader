package ru.terentjev.engine.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.terentjev.engine.Info;
import ru.terentjev.engine.Render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class PDFRender implements Render {

    private static final Logger log = LoggerFactory.getLogger(PDFRender.class);


    public Info getInfo(InputStream is) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        int pages = document.getNumberOfPages();
        PDDocumentInformation information = document.getDocumentInformation();
        String author = information.getAuthor();
        String title = information.getTitle();
        return new Info(pages, author, title);
    }

    public void render(InputStream is, List<Integer> pages) throws IOException {
        PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(is));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (Integer page : pages) {
            log.info("Rendering page {}", page);
            BufferedImage image = pdfRenderer.renderImageWithDPI(page - 1, 150);
            File outputFile = new File("./pages/page_" + page + ".png");
            ImageIO.write(image, "PNG", outputFile);
        }
    }
}
