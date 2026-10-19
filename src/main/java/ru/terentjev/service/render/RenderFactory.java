package ru.terentjev.service.render;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class RenderFactory {

    private final Map<String, Render> renders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public RenderFactory(List<Render> renders) {
        for (Render render : renders) {
            this.renders.put(render.type(), render);
        }
    }

    public Render getRender(String type) {
        Render render = renders.get(type);
        if (render == null) {
            throw new IllegalArgumentException("Unknown render type: " + type);
        }
        return render;
    }

    public List<String> settingsDefinitions(String type) {
        Render render = renders.get(type);
        if (render == null) {
            return List.of();
        }
        return render.settingsDefinitions();
    }

}
