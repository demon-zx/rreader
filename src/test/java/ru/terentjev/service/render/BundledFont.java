package ru.terentjev.service.render;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Встроенный шрифт для тестов рендеринга.
 * <p>
 * Шрифт лежит в тестовых ресурсах, поэтому рендеринг и golden-изображения
 * не зависят от того, какие шрифты установлены в системе.
 */
public final class BundledFont {
    private static final String DIRECTORY = "/fonts/liberation-sans/";
    private static final String[] FILES = {
            "LiberationSans-Regular.ttf",
            "LiberationSans-Bold.ttf",
            "LiberationSans-Italic.ttf",
            "LiberationSans-BoldItalic.ttf"
    };
    /**
     * Имя семейства встроенного шрифта. Обращение к полю регистрирует шрифт
     * в текущей JVM.
     */
    public static final String SANS = registerAll();

    private BundledFont() {
    }

    private static String registerAll() {
        for (String file : FILES) {
            register(DIRECTORY + file);
        }
        return "Liberation Sans";
    }

    private static void register(String resource) {
        try (InputStream is = BundledFont.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException("Font resource not found: " + resource);
            }
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .registerFont(Font.createFont(Font.TRUETYPE_FONT, is));
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Failed to register font " + resource, e);
        }
    }
}