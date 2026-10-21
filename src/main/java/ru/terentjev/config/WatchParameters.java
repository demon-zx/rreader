package ru.terentjev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Параметры отслеживания изменений в каталоге с книгами.
 *
 * @param enabled  включено ли отслеживание
 * @param debounce задержка перед обработкой файла после последнего события,
 *                 чтобы не читать файл во время записи (например «5s» — 5 секунд)
 */
@ConfigurationProperties(prefix = "application.watch")
public record WatchParameters(boolean enabled, Duration debounce) {
}
