#!/bin/sh
set -e

# UID/GID можно переопределить через env при docker run
USER_ID=${UID:-998}
GROUP_ID=${GID:-998}

# Создаём группу если нужно
if ! getent group "$GROUP_ID" >/dev/null 2>&1; then
    addgroup -g "$GROUP_ID" appgroup
fi
GROUP_NAME=$(getent group "$GROUP_ID" | cut -d: -f1)

# Создаём пользователя если нужно
if ! getent passwd "$USER_ID" >/dev/null 2>&1; then
    adduser -S -u "$USER_ID" -G "$GROUP_NAME" appuser
fi
USER_NAME=$(getent passwd "$USER_ID" | cut -d: -f1)

# ВАЖНО: меняем владельца ДО запуска приложения
# Используем || true чтобы не падать если нет прав
chown -R "${USER_NAME}:${GROUP_NAME}" /application 2>/dev/null || true
chown -R "${USER_NAME}:${GROUP_NAME}" /config 2>/dev/null || true

# Убедимся что права на запись есть
chmod -R u+rwX /config 2>/dev/null || true

# Запускаем от нужного пользователя
exec su-exec "${USER_NAME}:${GROUP_NAME}" "$@"
