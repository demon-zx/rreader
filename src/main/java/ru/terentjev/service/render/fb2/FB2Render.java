package ru.terentjev.service.render.fb2;

import org.springframework.stereotype.Component;

import ru.terentjev.service.render.Info;
import ru.terentjev.service.render.OpenInfo;
import ru.terentjev.service.render.Render;
import ru.terentjev.service.render.RenderSettings;
import ru.terentjev.service.render.fb2.layout.Drawer;
import ru.terentjev.service.render.fb2.layout.LayoutManager;
import ru.terentjev.service.render.fb2.layout.Page;
import ru.terentjev.service.render.fb2.parser.FB2;
import ru.terentjev.service.render.fb2.parser.FB2.Sequence;
import ru.terentjev.service.render.fb2.parser.FB2Parser;
import ru.terentjev.service.render.fb2.parser.FB2RenderSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class FB2Render implements Render {

    @Override
    public String type() {
        return "FB2";
    }

    private FB2RenderSettings settings(RenderSettings source) {
        return new FB2RenderSettings(
                source.viewportWidth(),
                source.viewportHeight(),
                50,
                50,
                50,
                50,
                "Arial",
                28,
                1.2f
        );
    }

    @Override
    public Info info(InputStream is) throws IOException {
        FB2Parser parser = new FB2Parser(Files.createTempDirectory("fb2-parser"));
        FB2 document = parser.parse(is);
        String author = document.authors()
            .stream()
            .findFirst()
            .orElse(null);
        Optional<Sequence> sequence = document.sequences()
            .stream()
            .findFirst();
        return new Info(
                author,
                document.title(),
                sequence.map(Sequence::name)
                        .orElse(null),
                sequence.map(Sequence::number)
                        .orElse(null)
        );
    }

    @Override
    public OpenInfo open(RenderSettings settings, InputStream is, Path bookDirectory, Path deviceDirectory) throws IOException {
        FB2Parser parser = new FB2Parser(bookDirectory);
        FB2 document = parser.parse(is);
        LayoutManager manager = new LayoutManager(settings(settings));
        List<Page> pages = manager.layout(document);
        String author = document.authors()
            .stream()
            .findFirst()
            .orElse(null);
        Optional<Sequence> sequence = document.sequences()
            .stream()
            .findFirst();
        Map<Integer, Integer> positionByPage = new HashMap<>();
        int i = 0;
        for (var page: pages) {
            positionByPage.put(++i, page.position());
        }
        return new OpenInfo(
                author,
                document.title(),
                pages.size(),
                sequence.map(Sequence::name)
                        .orElse(null),
                sequence.map(Sequence::number)
                        .orElse(null),
                positionByPage
        );
    }

    @Override
    public void cover(InputStream is, Path bookDirectory, OutputStream os) throws IOException {
        FB2Parser parser = new FB2Parser(bookDirectory);
        FB2 document = parser.parse(is);
        Path file = Optional.of(document.binaries())
            .map(s -> s.get(document.cover()))
            .filter(Objects::nonNull)
            .map(s -> Path.of(s))
            .filter(s -> Files.exists(s))
            .orElse(null);
        if (file != null) {
            Files.copy(file, os);
        } else {
            //TODO вставить NO IMAGE какой-нить
        }
        os.flush();
    }

    @Override
    public void render(RenderSettings settings, int page, InputStream is, Path bookDirectory, Path deviceDirectory, OutputStream os) throws IOException {
        FB2Parser parser = new FB2Parser(bookDirectory);
        FB2 document = parser.parse(is);
        var fb2settings = settings(settings);
        LayoutManager manager = new LayoutManager(fb2settings);
        Drawer drawer = new Drawer(fb2settings);
        List<Page> pages = manager.layout(document);
        int index = Math.min(Math.max(page - 1, 0), pages.size() - 1);
        drawer.draw(pages.get(index), os);
        os.flush();
    }

}
