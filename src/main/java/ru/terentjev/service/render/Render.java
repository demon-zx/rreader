package ru.terentjev.service.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface Render {

    String type();

    Info open(InputStream is, Path bookDirectory, Path deviceDirectory) throws IOException;

    void cover(InputStream is, Path bookDirectory, OutputStream os) throws IOException;

    void render(int page, InputStream is, Path bookDirectory, Path deviceDirectory, OutputStream os) throws IOException;

}
