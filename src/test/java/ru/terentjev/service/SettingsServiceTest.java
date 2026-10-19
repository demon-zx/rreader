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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThat(result.getScope()).isEqualTo("FB2");
        assertThat(keys(result)).containsExactlyInAnyOrder(
                "font", "size", "color", "background",
                "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "indent"
        );
        assertThat(valueOf(result, "font")).isEqualTo("Arial");
        assertThat(valueOf(result, "size")).isEqualTo("28");
        assertThat(valueOf(result, "color")).isEqualTo("#000000");
        assertThat(valueOf(result, "background")).isEqualTo("#FFFFFF");
        assertThat(valueOf(result, "paddingLeft")).isEqualTo("50");
        assertThat(valueOf(result, "paddingTop")).isEqualTo("50");
        assertThat(valueOf(result, "paddingRight")).isEqualTo("50");
        assertThat(valueOf(result, "paddingBottom")).isEqualTo("50");
        assertThat(valueOf(result, "indent")).isEqualTo("50");
    }

    @Test
    void definitionsPerScope() {
        List<String> fb2 = List.of(
                "font", "size", "color", "background",
                "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "indent"
        );
        when(renderFactory.settingsDefinitions("FB2")).thenReturn(fb2);

        assertThat(service.definitions(SettingsScope.FB2)).isSameAs(fb2);
        assertThat(service.definitions(SettingsScope.GLOBAL)).containsExactly(
                "font", "size", "color", "background",
                "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "indent"
        );
    }

    @Test
    void definitionsForScopeWithoutRenderAreEmpty() {
        assertThat(service.definitions(SettingsScope.PDF)).isEmpty();
    }

    @Test
    void definitionsRejectsNullScope() {
        assertThatThrownBy(() -> service.definitions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");
    }

    @Test
    void readFallsBackToGlobalWhenScopeKeyMissing() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL", "FB2")))
                .thenReturn(List.of(
                        entity("GLOBAL", "font", "Times New Roman"),
                        entity("GLOBAL", "size", "20")
                ));

        Settings result = service.read(DEVICE, SettingsScope.FB2);

        assertThat(valueOf(result, "font")).isEqualTo("Times New Roman");
        assertThat(valueOf(result, "size")).isEqualTo("20");
        assertThat(valueOf(result, "color")).isEqualTo("#000000");
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

        assertThat(valueOf(result, "font")).isEqualTo("Georgia");
        assertThat(valueOf(result, "size")).isEqualTo("28");
    }

    @Test
    void readGlobalScopeQueriesOnlyGlobal() {
        when(repository.findByDeviceUuidAndScopeIn(DEVICE, List.of("GLOBAL")))
                .thenReturn(List.of(entity("GLOBAL", "font", "Courier")));

        Settings result = service.read(DEVICE, SettingsScope.GLOBAL);

        assertThat(valueOf(result, "font")).isEqualTo("Courier");
    }

    @Test
    void readRejectsNullScope() {
        assertThatThrownBy(() -> service.read(DEVICE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");
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
        assertThat(saved.id()).isNull();
        assertThat(saved.deviceUuid()).isEqualTo(DEVICE);
        assertThat(saved.scope()).isEqualTo("FB2");
        assertThat(saved.key()).isEqualTo("font");
        assertThat(saved.value()).isEqualTo("Verdana");
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(saved.updatedBy()).isEqualTo("alice");
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
        assertThat(updated.id()).isEqualTo(SETTING_ID);
        assertThat(updated.value()).isEqualTo("Georgia");
        assertThat(updated.updatedAt()).isEqualTo(NOW);
        assertThat(updated.updatedBy()).isEqualTo("alice");
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
        assertThat(captor.getValue().key()).isEqualTo("size");
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
        assertThat(saved).anySatisfy(e -> {
            assertThat(e.key()).isEqualTo("font");
            assertThat(e.value()).isEqualTo("Georgia");
            assertThat(e.id()).isEqualTo(oldFont.id());
        });
        assertThat(saved).anySatisfy(e -> {
            assertThat(e.key()).isEqualTo("background");
            assertThat(e.value()).isEqualTo("#EEEEEE");
            assertThat(e.id()).isNull();
        });
    }

    @Test
    void saveRejectsNullScope() {
        assertThatThrownBy(() -> service.save(DEVICE, null, List.of(), "user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");
        verify(repository, never()).findByDeviceUuidAndScopeIn(any(), any());
    }

    @Test
    void saveRejectsNullValues() {
        assertThatThrownBy(() -> service.save(DEVICE, SettingsScope.GLOBAL, null, "user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("values");
    }

    @Test
    void saveRejectsBlankKey() {
        List<KeyValue> values = new ArrayList<>();
        values.add(new KeyValue("", "v"));
        assertThatThrownBy(() -> service.save(DEVICE, SettingsScope.GLOBAL, values, "user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
    }

    // endregion

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
