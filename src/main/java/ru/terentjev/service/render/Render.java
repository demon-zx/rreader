package ru.terentjev.service.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Render {
    String type();

    Info info(InputStream is) throws IOException;

    void render(InputStream is, List<Integer> pages) throws IOException;
}
