package ru.terentjev.service.render;

import java.util.Map;

public record RenderSettings(int viewportWidth, int viewportHeight, Map<String, String> settings) {
    public RenderSettings {
        settings = settings == null ? Map.of() : Map.copyOf(settings);
    }

    public RenderSettings(int viewportWidth, int viewportHeight) {
        this(viewportWidth, viewportHeight, Map.of());
    }

    public String value(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public int intValue(String key, int defaultValue) {
        String value = settings.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean booleanValue(String key, boolean defaultValue) {
        String value = settings.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return switch (value.trim()
                .toLowerCase()) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> defaultValue;
        };
    }
}
