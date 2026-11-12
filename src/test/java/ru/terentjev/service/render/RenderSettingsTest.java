package ru.terentjev.service.render;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderSettingsTest {
    @Test
    void valueReturnsConfiguredValue() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("font", "Georgia"));
        assertEquals("Georgia", settings.value("font", "Arial"));
    }

    @Test
    void valueReturnsDefaultForMissingKey() {
        RenderSettings settings = new RenderSettings(100, 200);
        assertEquals("Arial", settings.value("font", "Arial"));
    }

    @Test
    void valueWithoutDefaultReturnsNullForMissingKey() {
        RenderSettings settings = new RenderSettings(100, 200);
        assertNull(settings.value("font", null));
    }

    @Test
    void intValueParsesConfiguredNumber() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("size", "30"));
        assertEquals(30, settings.intValue("size", 28));
    }

    @Test
    void intValueTrimsWhitespace() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("size", "  30  "));
        assertEquals(30, settings.intValue("size", 28));
    }

    @Test
    void intValueReturnsDefaultForMissingOrBlankValue() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("blank", " "));
        assertEquals(28, settings.intValue("missing", 28));
        assertEquals(28, settings.intValue("blank", 28));
    }

    @Test
    void intValueReturnsDefaultForInvalidNumber() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("size", "big"));
        assertEquals(28, settings.intValue("size", 28));
    }

    @Test
    void booleanValueAcceptsTrueRepresentations() {
        for (String value : new String[]{
                "true", "1", "yes", "on", "TRUE", " Yes "
        }) {
            RenderSettings settings = new RenderSettings(100, 200, Map.of("justify", value));
            assertTrue(settings.booleanValue("justify", false), "expected '" + value + "' to be true");
        }
    }

    @Test
    void booleanValueAcceptsFalseRepresentations() {
        for (String value : new String[]{
                "false", "0", "no", "off", "FALSE", " Off "
        }) {
            RenderSettings settings = new RenderSettings(100, 200, Map.of("justify", value));
            assertFalse(settings.booleanValue("justify", true), "expected '" + value + "' to be false");
        }
    }

    @Test
    void booleanValueReturnsDefaultForMissingOrBlankValue() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("blank", ""));
        assertTrue(settings.booleanValue("missing", true));
        assertTrue(settings.booleanValue("blank", true));
    }

    @Test
    void booleanValueReturnsDefaultForUnknownValue() {
        RenderSettings settings = new RenderSettings(100, 200, Map.of("justify", "maybe"));
        assertTrue(settings.booleanValue("justify", true));
        assertFalse(settings.booleanValue("justify", false));
    }

    @Test
    void nullSettingsBecomeEmptyMap() {
        RenderSettings settings = new RenderSettings(100, 200, null);
        assertEquals("Arial", settings.value("font", "Arial"));
        assertTrue(settings.booleanValue("justify", true));
        assertEquals(28, settings.intValue("size", 28));
    }

    @Test
    void settingsMapIsCopied() {
        Map<String, String> source = new HashMap<>();
        source.put("font", "Georgia");
        RenderSettings settings = new RenderSettings(100, 200, source);
        source.put("font", "Arial");
        assertEquals("Georgia", settings.value("font", null));
    }
}