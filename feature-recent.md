# Функционал «Recent» (библиотека)

Секция «Recent» на странице библиотеки: от 0 до 4 книг, которые читаются
сейчас (у книги есть «текущая» закладка `$current$`), отсортированных по дате
изменения закладки (`updated_at`) по убыванию — в топ попадают книги,
открытые в последний раз. На карточке показан процент прочитанного.

Секция размещается сверху, над строкой поиска. Карточки того же размера, что
и в основном списке. Дублирование с основным списком допускается.

## План

### 1. Backend

- [x] `BookmarkRepositoryCurrent` + `BookmarkRepositoryCurrentImpl` —
      custom fragment (как `BookRepositorySearch`) с `JdbcAggregateTemplate`:
      `findCurrent(username, title, Pageable)` — `Pageable` в `@Query`
      методах Spring Data JDBC не применяется, поэтому используется
      `Criteria` + `Query.with(pageable)`.
- [x] `BookmarkService.findRecent(String username, int limit)` — обёртка над
      репозиторием: `PageRequest.of(0, limit, Sort.by("updatedAt").descending())`,
      возвращает `List<Bookmark>` (bookId + position) в порядке убывания
      `updated_at`.
- [x] `BookService.findByIds(Collection<UUID>)` — batch-загрузка книг через
      `findAllById`, сохраняется порядок, отсутствующие пропускаются.
- [x] `BookRecentService` — бизнес-логика «что читают сейчас»: берёт
      «текущие» закладки, подгружает книги, для каждой открывает
      `RenderService.open(...)` (нужны `deviceUuid`/`width`/`height`),
      получает `pages` и `positionByPage`, по текущей позиции закладки
      находит страницу и считает процент прочитанного. Возвращает
      `List<BookRecent>`.
- [x] `BookRecent extends Book` — модель с текущей позицией чтения
      (`position`), общим числом страниц (`pages`) и процентом (`percent`).
- [x] `LibraryController` — эндпоинт:
      `GET /api/v1/library/recent?deviceUuid=...&width=...&height=...&limit=4`
      → `List<ApiBookRecent>`, маппинг через `ApiBookRecentMapper`.
- [x] `ApiBookRecent extends ApiBook` + `ApiBookRecentMapper`.

Расчёт процента: позиция закладки — это «позиция сервера» (`positionByPage`),
по ней находится страница (аналог `positionToPage` в reader.js), а
процент = `страница / pages * 100`, не более 100. Для PDF, где
`positionByPage` пуст, номер страницы равен позиции.

### 2. Frontend

- [x] `library.html` — секция `<div class="row" id="recent">` между заголовком
      и строкой поиска (сверху над search), изначально скрыта.
- [x] `library.js`:
      - `loadRecent()` → `ajax('/api/v1/library/recent?deviceUuid=...&width=...&height=...&limit=4')`,
      - общая функция `bookCard(book, percent)` для карточек recent и
        основного списка; при наличии `percent` поверх обложки показывается
        бейдж с процентом прочитанного,
      - при 0 книг секция скрывается.
- [x] `library.css` — `.book-cover-wrap`/`.read-percent`: бейдж процента
      позиционируется поверх обложки.

### 3. Тесты

- [x] `BookmarkRepositoryTest.findCurrentOrdersByUpdatedAtDescRespectsUserAndLimit`
      — сортировка по `updated_at` desc, фильтр по пользователю, лимит.
- [x] `BookmarkServiceTest` — `findRecent`: порядок по `updated_at` desc,
      фильтр по пользователю и заголовку `$current$`, лимит, позиции.
- [x] `BookServiceTest` — `findByIds`: порядок, пропуск отсутствующих, пустой список.
- [x] `BookRecentServiceTest` — порядок, позиции, процент по `positionByPage`,
      clamp 100%, PDF (пустой `positionByPage`), 0 страниц, пропуск
      отсутствующих книг, пустой список.
- [x] `LibraryControllerTest` — recent-эндпоинт: поля `position`/`pages`/
      `percent` в ответе, лимит, пустой список, 400 без `deviceUuid`,
      500 при ошибке сервиса.
