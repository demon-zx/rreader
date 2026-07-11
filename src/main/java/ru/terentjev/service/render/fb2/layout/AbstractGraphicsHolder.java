package ru.terentjev.service.render.fb2.layout;

import org.apache.commons.lang3.function.FailableFunction;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

public abstract class AbstractGraphicsHolder {

    protected <T> T withGraphics(
        BufferedImage image, 
        FailableFunction<Graphics2D, T, IOException> graphics
    ) throws IOException {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        try {
            return graphics.apply(g);
        } finally {
            g.dispose();
        }
    }

}
