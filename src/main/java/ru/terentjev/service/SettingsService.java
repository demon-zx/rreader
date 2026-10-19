package ru.terentjev.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.terentjev.repository.SettingsRepository;
import ru.terentjev.repository.model.SettingsEntity;
import ru.terentjev.service.mapper.SettingsMapper;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.SettingKeys;
import ru.terentjev.service.model.Settings;
import ru.terentjev.service.model.SettingsScope;
import ru.terentjev.service.render.RenderFactory;

import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private static final List<String> GLOBAL_DEFINITIONS = List.of(
            SettingKeys.FONT,
            SettingKeys.SIZE,
            SettingKeys.COLOR,
            SettingKeys.BACKGROUND,
            SettingKeys.PADDING_LEFT,
            SettingKeys.PADDING_TOP,
            SettingKeys.PADDING_RIGHT,
            SettingKeys.PADDING_BOTTOM,
            SettingKeys.INDENT
    );

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry(SettingKeys.FONT, "Arial"),
            Map.entry(SettingKeys.SIZE, "28"),
            Map.entry(SettingKeys.COLOR, "#000000"),
            Map.entry(SettingKeys.BACKGROUND, "#FFFFFF"),
            Map.entry(SettingKeys.PADDING_LEFT, "50"),
            Map.entry(SettingKeys.PADDING_TOP, "50"),
            Map.entry(SettingKeys.PADDING_RIGHT, "50"),
            Map.entry(SettingKeys.PADDING_BOTTOM, "50"),
            Map.entry(SettingKeys.INDENT, "50")
    );

    private final SettingsRepository repository;
    private final Clock clock;
    private final RenderFactory renderFactory;
    private final ApplicationEventPublisher publisher;

    public SettingsService(
            SettingsRepository repository,
            Clock clock,
            RenderFactory renderFactory,
            ApplicationEventPublisher publisher
    ) {
        this.repository = repository;
        this.clock = clock;
        this.renderFactory = renderFactory;
        this.publisher = publisher;
    }

    public List<String> definitions(SettingsScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        if (scope == SettingsScope.GLOBAL) {
            return GLOBAL_DEFINITIONS;
        }
        return renderFactory.settingsDefinitions(scope.name());
    }

    public List<String> fonts() {
        return Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()
        );
    }

    /**
     * Чтение эффективных настроек с иерархией: scope → GLOBAL → дефолтные значения.
     */
    public Settings read(UUID deviceUuid, SettingsScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        List<String> scopes;
        if (scope == SettingsScope.GLOBAL) {
            scopes = List.of(SettingsScope.GLOBAL.name());
        } else {
            scopes = List.of(SettingsScope.GLOBAL.name(), scope.name());
        }
        List<SettingsEntity> entities = repository.findByDeviceUuidAndScopeIn(deviceUuid, scopes);
        Map<String, String> merged = new LinkedHashMap<>(DEFAULTS);
        Map<String, List<SettingsEntity>> byScope = entities.stream()
                .collect(Collectors.groupingBy(SettingsEntity::scope));
        for (String s : scopes) {
            for (SettingsEntity e : byScope.getOrDefault(s, List.of())) {
                merged.put(e.key(), e.value());
            }
        }
        List<KeyValue> settings = merged.entrySet()
                .stream()
                .map(e -> new KeyValue(e.getKey(), e.getValue()))
                .toList();
        return new Settings(scope.name(), settings);
    }

    /**
     * Сохранение настроек
     */
    @Transactional
    public void save(UUID deviceUuid, SettingsScope scope, List<KeyValue> values, String username) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        Instant now = clock.instant();
        String scopeName = scope.name();
        List<SettingsEntity> existing = repository.findByDeviceUuidAndScopeIn(
                deviceUuid, List.of(scopeName)
        );
        Map<String, SettingsEntity> byKey = existing.stream()
                .collect(Collectors.toMap(SettingsEntity::key, e -> e));
        Map<String, String> incoming = new HashMap<>();
        for (KeyValue kv : values) {
            if (StringUtils.isBlank(kv.getKey())) {
                throw new IllegalArgumentException("setting key must not be blank");
            }
            incoming.put(kv.getKey(), kv.getValue());
        }
        for (String key : byKey.keySet()) {
            if (!incoming.containsKey(key)) {
                repository.delete(byKey.get(key));
            }
        }
        for (Map.Entry<String, String> entry : incoming.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            SettingsEntity old = byKey.get(key);
            if (old == null || !value.equals(old.value())) {
                UUID id = null;
                if (old != null) {
                    id = old.id();
                }
                SettingsEntity entity = new SettingsEntity(id, deviceUuid, scopeName, key, value, now, username);
                repository.save(entity);
            }
        }
        publisher.publishEvent(new SettingsChangedEvent(deviceUuid));
    }

}
