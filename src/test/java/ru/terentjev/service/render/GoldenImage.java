package ru.terentjev.service.render;

import org.junit.jupiter.api.Assertions;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сравнение отрендеренных изображений с эталонами из {@code src/test/resources/golden}.
 * <p>
 * Чтобы перегенерировать эталоны, тесты нужно запустить с системным свойством
 * {@code -Dgolden.update=true}. В этом режиме сравнение не выполняется, а текущий
 * результат сохраняется на место эталона.
 */
public final class GoldenImage {
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");
    private static final Path GOLDEN_DIRECTORY = Path.of("src", "test", "resources", "golden");
    private static final Path DIFF_DIRECTORY = Path.of("target", "golden-diff");
    private static final String UPDATE_HINT = "Run tests with -Dgolden.update=true to regenerate golden images.";

    private GoldenImage() {
    }

    /**
     * Сравнивает изображение с эталоном. При расхождении сохраняет фактический
     * результат в {@code target/golden-diff} и падает с описанием отличий.
     *
     * @param name   путь эталона относительно каталога {@code golden}, например {@code fb2/page.png}
     * @param actual фактическое изображение
     */
    public static void assertMatchesGolden(String name, BufferedImage actual) throws IOException {
        if (UPDATE) {
            save(GOLDEN_DIRECTORY.resolve(name), actual);
            return;
        }
        BufferedImage expected = read(name);
        if (expected == null) {
            Assertions.fail("Golden image not found: " + GOLDEN_DIRECTORY.resolve(name) + ". " + UPDATE_HINT);
        }
        Assertions.assertEquals(expected.getWidth(), actual.getWidth(), "width of golden image " + name);
        Assertions.assertEquals(expected.getHeight(), actual.getHeight(), "height of golden image " + name);
        int differentPixels = 0;
        int maxDelta = 0;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int delta = maxChannelDelta(expected.getRGB(x, y), actual.getRGB(x, y));
                if (delta > 0) {
                    differentPixels++;
                    maxDelta = Math.max(maxDelta, delta);
                }
            }
        }
        if (differentPixels > 0) {
            Path actualFile = save(DIFF_DIRECTORY.resolve(name), actual);
            Assertions.fail(
                    "Image differs from golden " + name +
                            ": " +
                            differentPixels +
                            " pixel(s), max channel delta " +
                            maxDelta +
                            ". Actual image saved to " +
                            actualFile +
                            ". " +
                            UPDATE_HINT
            );
        }
    }

    /**
     * Читает эталон из исходников, а если файла там нет — из classpath.
     *
     * @param name путь эталона относительно каталога {@code golden}
     * @return изображение или {@code null}, если эталон не найден
     */
    public static BufferedImage read(String name) throws IOException {
        Path file = GOLDEN_DIRECTORY.resolve(name);
        if (Files.exists(file)) {
            return ImageIO.read(file.toFile());
        }
        try (InputStream is = GoldenImage.class.getResourceAsStream("/golden/" + name)) {
            return is == null ? null : ImageIO.read(is);
        }
    }

    private static Path save(Path file, BufferedImage image) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(image, "PNG", file.toFile());
        return file;
    }

    private static int maxChannelDelta(int first, int second) {
        int red = Math.abs(((first >> 16) & 0xFF) - ((second >> 16) & 0xFF));
        int green = Math.abs(((first >> 8) & 0xFF) - ((second >> 8) & 0xFF));
        int blue = Math.abs((first & 0xFF) - (second & 0xFF));
        return Math.max(red, Math.max(green, blue));
    }
}