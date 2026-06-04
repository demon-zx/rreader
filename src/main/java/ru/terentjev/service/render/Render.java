package ru.terentjev.service.render;

import org.apache.commons.lang3.function.FailableBiConsumer;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface Render {
    String type();

    Info info(InputStream is) throws IOException;

    void cover(
            InputStream is,
            OutputStream os
    ) throws IOException;

    void render(
            int page,
            InputStream is,
            OutputStream os
    ) throws IOException;
}
