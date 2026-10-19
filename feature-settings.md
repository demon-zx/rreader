# Функционал «Настройки»

Настройки чтения: имя шрифта, размер, цвет текста, цвет фона, отступы,
отступ первой строки параграфа (`indent`).
Каждая настройка имеет область действия (scope): `GLOBAL`, `FB2`, `PDF`, ...
Настройки привязаны к устройству (`deviceUuid`) и пользователю. В базе хранятся
дата изменения и автор.

Чтение с иерархией: ищем настройку в настройках формата (fb2, pdf, ...) →
далее `GLOBAL` → если ничего не найдено, дефолтные значения.

## Готово

- [x] `SettingsController` (`ru.terentjev.controller`) — `GET /api/v1/settings/fonts`
- [x] `SettingsService` — метод `fonts()` возвращает реальные шрифты системы (`GraphicsEnvironment`)
- [x] Модели `service.model.Settings` (scope, List\<KeyValue\>) и `service.model.KeyValue`
- [x] Страница настроек: `SettingsHtmlController` (`GET /settings`), `templates/settings.html`,
      `static/js/settings.js`, `static/css/settings.css`; выбор шрифта и размера,
      пока временно сохраняется в localStorage

## План

### 1. Модель данных (БД)

- [x] Liquibase change set: таблица `settings`
  - `id` uuid PK
  - `device_uuid` uuid NOT NULL
  - `scope` varchar(64) NOT NULL
  - `setting_key` varchar(128) NOT NULL
  - `setting_value` varchar(256) NOT NULL
  - `updated_at` timestamp
  - `updated_by` varchar(128) — автор изменения (username)
  - unique: (`device_uuid`, `scope`, `setting_key`)
  - вариант с сохранением настроек JSON-колонкой — рассмотреть как альтернативу
- [x] `SettingsEntity` + `SettingsRepository`:
  - delete/replace по (deviceUuid, scope) при сохранении
- [x] `SettingsMapper` (MapStruct: entity ⇄ KeyValue/Settings)

### 2. Слой модели/сервис

- [x] `SettingsScope` (enum: `GLOBAL`, `FB2`, `PDF`)
  вместо строкового scope в `SettingsController`/сервисе
- [x] Дефолтные значения: `Arial`, 28, `#000000`, `#FFFFFF`, отступы 50
  (текущие хардкод-значения из `FB2Render`/`LayoutManager`)
- [x] `SettingsService.read(UUID deviceUuid, SettingsScope scope)`:
  иерархическое чтение (формат → GLOBAL → дефолты) + слияние по-ключам:
  отсутствует ключ — берём из нижней ступени иерархии
- [x] `SettingsService.save(UUID deviceUuid, SettingsScope scope,
  List<KeyValue> values, String username)`:
  запись/обновление, заполнение `updated_at` / `updated_by`

### 3. REST API

- [x] `GET /api/v1/settings?deviceUuid=&scope=` → эффективные настройки
      (результат иерархического чтения, `Settings` / List\<KeyValue\>)
- [x] `PUT /api/v1/settings?deviceUuid=&scope=` (body: List\<KeyValue\>) →
      сохранение; `updated_by` — из сессии (username)
- [x] Проверка scope/ключей — `InvalidArgumentException` ⇒ 400

### 4. Интеграция с рендером

- [x] `RenderService.open/render` получать эффективные настройки для
      (deviceUuid, scope по формату книги) вместо хардкода
      (сейчас: `new FB2RenderSettings(1080, 1527, 50, 50, 50, 50, "Arial", 28, 1.2f)`)
- [x] Подставить color/background/padding в layout FB2/PDF
- [x] Сброс кеша страниц при изменении настроек: `SettingsService.save` публикует
      `SettingsChangedEvent`, `RenderService` удаляет директории устройства
      (`pages/<book>/<deviceUuid>`)

### 5. Фронт

- [x] `settings.js`: загрузка/сохранение через API вместо localStorage
- [x] `settings.html`: поля «color», «background», «padding»
      (select цвета — палитра; числовое поле размера — уже есть)
- [x] Передача `deviceUuid` (`common.js#getDeviceUuid`) в запросы настроек

### 6. Тесты

- [x] Unit: иерархическое чтение и слияние (`fb2 → global → default`)
- [x] Unit: сохранение, заполнение updated_at/updated_by
- [x] API: GET/PUT `/api/v1/settings` (400 при некорректных параметрах)
- [x] Рендер: страница с применёнными настройками отличается от дефолтных
      (`DrawerTest`, `RenderServiceTest`)

### 7. Дальнейшие шаги

- [x] Возврат с сервера списка возможных настроек: `GET /api/v1/settings/definitions?scope=`
      (список ключей); набор определяется конкретным `Render` (`Render.definitions()`,
      у FB2 и PDF свой), `GLOBAL` — в `SettingsService`; все настройки расположены в html
      (атрибут `data-setting`), показываются только присланные сервером —
      разметка у каждой настройки своя
- [x] `padding` разбит на `paddingLeft`/`paddingTop`/`paddingRight`/`paddingBottom`
- [x] Переход в настройки со страницы reader: пункт меню «Настройки» →
      `/settings?scope=<тип книги>`; scope выбирается из параметра URL
