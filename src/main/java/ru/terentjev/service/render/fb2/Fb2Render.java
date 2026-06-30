package ru.terentjev.service.render.fb2;

import org.springframework.stereotype.Component;
import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.Render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * {@link Render} implementation for the FB2 format. All three operations stream the file exactly
 * once through {@link Fb2Parser}; pages are produced on demand by {@link Fb2Painter} and cached as
 * PNG by the surrounding render pipeline.
 */
@Component
public class Fb2Render implements Render {

    private final Fb2Parser parser;
    private final Fb2Painter painter = new Fb2Painter();

    public Fb2Render(Fb2Parser parser) {
        this.parser = parser;
    }

    @Override
    public String type() {
        return "FB2";
    }

    @Override
    public Info info(InputStream is) throws IOException {
        Fb2Document document = parser.parse(is);
        Fb2Metadata metadata = document.metadata();
        int count = painter.layout(document.blocks()).size();
        return new Info(Math.max(count, 1), metadata.author(), metadata.title());
    }

    @Override
    public void cover(InputStream is, OutputStream os) throws IOException {
        Fb2Document document = parser.parse(is);
        String href = document.metadata().coverHref();
        byte[] cover = href == null ? null : document.binary(href);
        if (cover != null && writeRaster(cover, os)) {
            return;
        }
        List<Fb2Painter.Page> pages = painter.layout(document.blocks());
        painter.draw(pages.get(0), document, os);
    }

    @Override
    public void render(int page, InputStream is, OutputStream os) throws IOException {
        Fb2Document document = parser.parse(is);
        List<Fb2Painter.Page> pages = painter.layout(document.blocks());
        int index = Math.min(Math.max(page - 1, 0), pages.size() - 1);
        painter.draw(pages.get(index), document, os);
    }

    private boolean writeRaster(byte[] data, OutputStream os) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            return false;
        }
        ImageIO.write(image, "PNG", os);
        os.flush();
        return true;
    }
}
