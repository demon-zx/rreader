package ru.terentjev.service.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface Render {

    String type();

    Info info(InputStream is) throws IOException;

    OpenInfo open(RenderSettings settings, InputStream is, Path bookDirectory, Path deviceDirectory) throws IOException;

    void cover(InputStream is, Path bookDirectory, OutputStream os) throws IOException;

    void render(RenderSettings settings, int page, InputStream is, Path bookDirectory, Path deviceDirectory, OutputStream os) throws IOException;

}
