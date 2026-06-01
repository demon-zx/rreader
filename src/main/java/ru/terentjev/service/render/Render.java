package ru.terentjev.service.render;

import org.apache.commons.lang3.function.FailableBiConsumer;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Render {
    String type();

    Info info(InputStream is) throws IOException;

    void render(
            InputStream is,
            FailableBiConsumer<RenderedImage, Integer, IOException> renderedPageConsumer,
            List<Integer> pages
    ) throws IOException;
}
