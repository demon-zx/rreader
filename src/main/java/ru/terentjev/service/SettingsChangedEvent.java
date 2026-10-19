package ru.terentjev.service;

import java.util.UUID;

/**
 * Настройки устройства изменены — рендер должен сбросить кеш страниц устройства.
 */
public record SettingsChangedEvent(UUID deviceUuid) {
}
