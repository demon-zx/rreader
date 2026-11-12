package ru.terentjev.service.render.fb2.layout;

import org.junit.jupiter.api.Test;

import ru.terentjev.service.render.BundledFont;
import ru.terentjev.service.render.fb2.layout.Page.LineObject;
import ru.terentjev.service.render.fb2.layout.Page.PageObject;
import ru.terentjev.service.render.fb2.layout.Page.Word;
import ru.terentjev.service.render.fb2.parser.FB2RenderSettings;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawerTest {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 100;

    @Test
    void defaultPageIsBlackOnWhite() throws IOException {
        BufferedImage rendered = draw(page("Hello world"), settings(Color.BLACK, Color.WHITE));
        assertEquals(Color.WHITE.getRGB(), rendered.getRGB(1, 1));
        assertTrue(containsColor(rendered, Color.BLACK.getRGB()));
    }

    @Test
    void pageWithAppliedSettingsDiffersFromDefault() throws IOException {
        Page page = page("Hello world");
        Color color = new Color(178, 34, 34);
        Color background = new Color(245, 245, 220);
        BufferedImage defaultPage = draw(page, settings(Color.BLACK, Color.WHITE));
        BufferedImage customPage = draw(page, settings(color, background));
        assertNotEquals(defaultPage.getRGB(1, 1), customPage.getRGB(1, 1));
        assertEquals(background.getRGB(), customPage.getRGB(1, 1));
        assertTrue(containsColor(customPage, color.getRGB()));
    }

    private static FB2RenderSettings settings(Color color, Color background) {
        return new FB2RenderSettings(
                WIDTH,
                HEIGHT,
                10,
                10,
                10,
                10,
                0,
                BundledFont.SANS,
                16,
                1.2f,
                color,
                background,
                true,
                true
        );
    }

    private static Page page(String text) {
        Font font = new Font(BundledFont.SANS, Font.PLAIN, 16);
        List<PageObject> objects = List.of(
                new LineObject(HEIGHT - 10, List.of(new Word(10, text, font)))
        );
        return new Page(0, objects);
    }

    private static BufferedImage draw(Page page, FB2RenderSettings settings) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new Drawer(settings).draw(page, os);
        return ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    private static boolean containsColor(BufferedImage image, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == rgb) {
                    return true;
                }
            }
        }
        return false;
    }
}
