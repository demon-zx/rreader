package ru.terentjev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Параметры чистки дискового кеша страниц по расписанию.
 *
 * @param enabled включена ли чистка
 * @param cron    расписание запуска (spring @Scheduled cron)
 * @param maxAge  максимальный возраст файла кеша; более старые удаляются
 *                (например «30d» — 30 дней)
 */
@ConfigurationProperties(prefix = "application.cache.cleanup")
public record CacheCleanupParameters(boolean enabled, String cron, Duration maxAge) {
}
