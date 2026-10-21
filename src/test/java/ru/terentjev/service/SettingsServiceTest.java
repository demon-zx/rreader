package ru.terentjev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import ru.terentjev.repository.SettingsRepository;
import ru.terentjev.repository.model.SettingsEntity;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.Settings;
import ru.terentjev.service.model.SettingsScope;
import ru.terentjev.service.render.RenderFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    private static final UUID DEVICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SETTING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @Mock
    private SettingsRepository repository;

    @Mock
    private RenderFactory renderFactory;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private SettingsService service;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneId.of("UTC"));
        service = new SettingsService(repository, clock, renderFactory, publisher);
    }

    // region read

    @Test
    void readReturnsDefaultsWhenNothingSaved() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL", "FB2")))
                .thenReturn(List.of());

        Settings result = service.read(DEVICE, SettingsScope.FB2);

        assertEquals("FB2", result.getScope());
        assertContainsInAnyOrder(List.of(
                "font", "size", "color", "background",
                "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "indent"
        ), keys(result));
        assertEquals("Arial", valueOf(result, "font"));
        assertEquals("28", valueOf(result, "size"));
        assertEquals("#000000", valueOf(result, "color"));
        assertEquals("#FFFFFF", valueOf(result, "background"));
        assertEquals("50", valueOf(result, "paddingLeft"));
        assertEquals("50", valueOf(result, "paddingTop"));
        assertEquals("50", valueOf(result, "paddingRight"));
        assertEquals("50", valueOf(result, "paddingBottom"));
        assertEquals("50", valueOf(result, "indent"));
    }

    @Test
    void definitionsPerScope() {
        List<String> fb2 = List.of(
                "font", "size", "color", "background",
                "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "indent"
        );
        when(renderFactory.settingsDefinitions("FB2")).thenReturn(fb2);

        assertSame(fb2, service.definitions(SettingsScope.FB2));
        assertIterableEquals(List.of(
                "font", "size", "color", "background",
                "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "indent"
        ), service.definitions(SettingsScope.GLOBAL));
    }

    @Test
    void definitionsForScopeWithoutRenderAreEmpty() {
        assertTrue(service.definitions(SettingsScope.PDF).isEmpty());
    }

    @Test
    void definitionsRejectsNullScope() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.definitions(null));
        assertTrue(e.getMessage().contains("scope"));
    }

    @Test
    void readFallsBackToGlobalWhenScopeKeyMissing() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL", "FB2")))
                .thenReturn(List.of(
                        entity("GLOBAL", "font", "Times New Roman"),
                        entity("GLOBAL", "size", "20")
                ));

        Settings result = service.read(DEVICE, SettingsScope.FB2);

        assertEquals("Times New Roman", valueOf(result, "font"));
        assertEquals("20", valueOf(result, "size"));
        assertEquals("#000000", valueOf(result, "color"));
    }

    @Test
    void readScopeOverridesGlobal() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL", "FB2")))
                .thenReturn(List.of(
                        entity("GLOBAL", "font", "Arial"),
                        entity("GLOBAL", "size", "28"),
                        entity("FB2", "font", "Georgia")
                ));

        Settings result = service.read(DEVICE, SettingsScope.FB2);

        assertEquals("Georgia", valueOf(result, "font"));
        assertEquals("28", valueOf(result, "size"));
    }

    @Test
    void readGlobalScopeQueriesOnlyGlobal() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL")))
                .thenReturn(List.of(entity("GLOBAL", "font", "Courier")));

        Settings result = service.read(DEVICE, SettingsScope.GLOBAL);

        assertEquals("Courier", valueOf(result, "font"));
    }

    @Test
    void readRejectsNullScope() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class, () -> service.read(DEVICE, null)
        );
        assertTrue(e.getMessage().contains("scope"));
    }

    // endregion

    // region save — diff logic

    @Test
    void saveInsertsNewKeys() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("FB2")))
                .thenReturn(List.of());

        service.save(DEVICE, SettingsScope.FB2, List.of(new KeyValue("font", "Verdana")), "alice");

        ArgumentCaptor<SettingsEntity> captor = ArgumentCaptor.forClass(SettingsEntity.class);
        verify(repository, times(1)).save(captor.capture());
        SettingsEntity saved = captor.getValue();
        assertNull(saved.id());
        assertEquals(DEVICE, saved.deviceUuid());
        assertEquals("FB2", saved.scope());
        assertEquals("font", saved.key());
        assertEquals("Verdana", saved.value());
        assertEquals(NOW, saved.updatedAt());
        assertEquals("alice", saved.updatedBy());
        verify(repository, never()).delete(any(SettingsEntity.class));
        verify(publisher).publishEvent(new SettingsChangedEvent(DEVICE));
    }

    @Test
    void saveUpdatesChangedKeysPreservingId() {
        SettingsEntity existing = new SettingsEntity(
                SETTING_ID, DEVICE, "FB2", "font", "Arial", Instant.parse("2024-01-01T00:00:00Z"), "old"
        );
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("FB2")))
                .thenReturn(List.of(existing));

        service.save(DEVICE, SettingsScope.FB2, List.of(new KeyValue("font", "Georgia")), "alice");

        ArgumentCaptor<SettingsEntity> captor = ArgumentCaptor.forClass(SettingsEntity.class);
        verify(repository, times(1)).save(captor.capture());
        SettingsEntity updated = captor.getValue();
        assertEquals(SETTING_ID, updated.id());
        assertEquals("Georgia", updated.value());
        assertEquals(NOW, updated.updatedAt());
        assertEquals("alice", updated.updatedBy());
        verify(repository, never()).delete(any(SettingsEntity.class));
    }

    @Test
    void saveSkipsUnchangedKeys() {
        SettingsEntity existing = new SettingsEntity(
                SETTING_ID, DEVICE, "GLOBAL", "size", "28", Instant.parse("2024-01-01T00:00:00Z"), "old"
        );
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL")))
                .thenReturn(List.of(existing));

        service.save(DEVICE, SettingsScope.GLOBAL, List.of(new KeyValue("size", "28")), "alice");

        verify(repository, never()).save(any(SettingsEntity.class));
        verify(repository, never()).delete(any(SettingsEntity.class));
    }

    @Test
    void saveDeletesKeysNotInIncoming() {
        SettingsEntity existing = new SettingsEntity(
                SETTING_ID, DEVICE, "PDF", "font", "Arial", Instant.parse("2024-01-01T00:00:00Z"), "old"
        );
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("PDF")))
                .thenReturn(List.of(existing));

        service.save(DEVICE, SettingsScope.PDF, List.of(new KeyValue("size", "20")), "bob");

        verify(repository, times(1)).delete(existing);
        ArgumentCaptor<SettingsEntity> captor = ArgumentCaptor.forClass(SettingsEntity.class);
        verify(repository, times(1)).save(captor.capture());
        assertEquals("size", captor.getValue().key());
    }

    @Test
    void savePerformsMixedDiff() {
        SettingsEntity oldFont = new SettingsEntity(
                UUID.randomUUID(), DEVICE, "FB2", "font", "Arial", Instant.parse("2024-01-01T00:00:00Z"), "old"
        );
        SettingsEntity oldSize = new SettingsEntity(
                UUID.randomUUID(), DEVICE, "FB2", "size", "28", Instant.parse("2024-01-01T00:00:00Z"), "old"
        );
        SettingsEntity oldColor = new SettingsEntity(
                UUID.randomUUID(), DEVICE, "FB2", "color", "#000000", Instant.parse("2024-01-01T00:00:00Z"), "old"
        );
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("FB2")))
                .thenReturn(List.of(oldFont, oldSize, oldColor));

        // font — изменено, size — без изменений, color — удалён, background — новый
        service.save(DEVICE, SettingsScope.FB2, List.of(
                new KeyValue("font", "Georgia"),
                new KeyValue("size", "28"),
                new KeyValue("background", "#EEEEEE")
        ), "alice");

        verify(repository, times(1)).delete(oldColor);

        ArgumentCaptor<SettingsEntity> captor = ArgumentCaptor.forClass(SettingsEntity.class);
        verify(repository, times(2)).save(captor.capture());
        List<SettingsEntity> saved = captor.getAllValues();
        SettingsEntity savedFont = findByKey(saved, "font");
        assertEquals("Georgia", savedFont.value());
        assertEquals(oldFont.id(), savedFont.id());
        SettingsEntity savedBackground = findByKey(saved, "background");
        assertEquals("#EEEEEE", savedBackground.value());
        assertNull(savedBackground.id());
    }

    @Test
    void saveRejectsNullScope() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class, () -> service.save(DEVICE, null, List.of(), "user")
        );
        assertTrue(e.getMessage().contains("scope"));
        verify(repository, never()).findByDeviceUuidAndScopeIn(any(), any());
    }

    @Test
    void saveRejectsNullValues() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class, () -> service.save(DEVICE, SettingsScope.GLOBAL, null, "user")
        );
        assertTrue(e.getMessage().contains("values"));
    }

    @Test
    void saveRejectsBlankKey() {
        List<KeyValue> values = new ArrayList<>();
        values.add(new KeyValue("", "v"));
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class, () -> service.save(DEVICE, SettingsScope.GLOBAL, values, "user")
        );
        assertTrue(e.getMessage().contains("key"));
    }

    // endregion

    private static void assertContainsInAnyOrder(List<String> expected, List<String> actual) {
        List<String> sortedExpected = new ArrayList<>(expected);
        List<String> sortedActual = new ArrayList<>(actual);
        Collections.sort(sortedExpected);
        Collections.sort(sortedActual);
        assertIterableEquals(sortedExpected, sortedActual);
    }

    private static SettingsEntity findByKey(List<SettingsEntity> entities, String key) {
        return entities.stream()
                .filter(e -> e.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static SettingsEntity entity(String scope, String key, String value) {
        return new SettingsEntity(null, DEVICE, scope, key, value, null, null);
    }

    private static List<String> keys(Settings settings) {
        return settings.getSettings().stream().map(KeyValue::getKey).toList();
    }

    private static String valueOf(Settings settings, String key) {
        return settings.getSettings().stream()
                .filter(kv -> kv.getKey().equals(key))
                .map(KeyValue::getValue)
                .findFirst()
                .orElseThrow();
    }
}
