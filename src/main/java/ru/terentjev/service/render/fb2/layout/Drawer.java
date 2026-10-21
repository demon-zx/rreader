package ru.terentjev.service.render.fb2.layout;

import ru.terentjev.service.render.fb2.layout.Page.ImageObject;
import ru.terentjev.service.render.fb2.layout.Page.LineObject;
import ru.terentjev.service.render.fb2.layout.Page.PageObject;
import ru.terentjev.service.render.fb2.layout.Page.Word;
import ru.terentjev.service.render.fb2.parser.FB2RenderSettings;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class Drawer extends AbstractGraphicsHolder {
    private final FB2RenderSettings settings;

    public Drawer(FB2RenderSettings settings) {
        this.settings = settings;
    }

    public void draw(Page page, OutputStream os) throws IOException {
        BufferedImage image = new BufferedImage(
                settings.pageWidth(),
                settings.pageHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        withGraphics(image, g -> {
            g.setColor(settings.background());
            g.fillRect(0, 0, settings.pageWidth(), settings.pageHeight());
            g.setColor(settings.color());
            for (PageObject object : page.objects()) {
                switch (object) {
                    case ImageObject o -> drawImage(g, o);
                    case LineObject o -> drawLine(g, o);
                    default -> throw new IllegalArgumentException("Unknown type: " + object.getClass());
                }
            }
            return null;
        });
        ImageIO.write(image, "PNG", os);
        os.flush();
    }

    private void drawLine(Graphics2D graphics, LineObject line) {
        for (Word word : line.words()) {
            graphics.setFont(word.font());
            graphics.drawString(word.value(), word.x(), line.y());
        }
    }

    private void drawImage(Graphics2D graphics, ImageObject o) throws IOException {
        Path pathImage = Path.of(o.href());
        if (Files.exists(pathImage)) {
            BufferedImage image;
            try (var is = Files.newInputStream(pathImage)) {
                image = ImageIO.read(is);
            }
            int areaX = settings.paddingLeft();
            int areaY = settings.paddingTop();
            int areaWidth = settings.pageWidth() - settings.paddingLeft() - settings.paddingRight();
            int areaHeight = settings.pageHeight() - settings.paddingTop() - settings.paddingBottom();
            double scale = Math.min(
                    (double) areaWidth / image.getWidth(),
                    (double) areaHeight / image.getHeight()
            );
            // не увеличиваем картинку, если она меньше области
            scale = Math.min(scale, 1.0);
            int w = (int) Math.round(image.getWidth() * scale);
            int h = (int) Math.round(image.getHeight() * scale);
            int x = areaX + (areaWidth - w) / 2;
            int y = areaY + (areaHeight - h) / 2;
            graphics.drawImage(image, x, y, w, h, null);
        }
    }
}
