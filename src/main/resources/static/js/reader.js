import { ajax, debounce, HttpError, getDeviceUuid } from "./common.js";

const deviceUuid = getDeviceUuid();

const VIEWPORT_WIDTH_KEY = 'rreader_viewport_width';
const MAGNIFIER_KEY = 'rreader_magnifier';
const FIT_WIDTH_KEY = 'rreader_fit_width';
const MAGNIFIER_ZOOM = 2;
const MAGNIFIER_DRAG_THRESHOLD = 4;
const MAGNIFIER_CLICK_SUPPRESS_TIMEOUT = 300;
const PAGE_TURN_DRAG_THRESHOLD = 40;
const CLICK_SUPPRESS_TIMEOUT = 300;

let eBook;
let eScroll;
let eMenu;
let eMenuTrigger;
let eMenuWidth100;
let eMenuWidth50;
let eMenuFitWidth;
let eMenuMagnifier;
let eMagnifier;
let eMagnifierView;
let eMagnifierFrame;
let menuTimeout;
let baseBookUrl;
let baseBookmarkUrl;
let eView;
let eAuthor;
let ePage;
let eTitle;
let eLoader;
let flashTimeout;
let currentPage = 1;
let pages = 0;
let positionByPage;
let onFocusNotInit = true;
let viewportWidthPercent = 100;
let magnifier = false;
let fitWidth = false;
let bookType = null;
let magnifierPoint = null;
let magnifierDrag = null;
let suppressMagnifierClick = false;
let suppressMagnifierClickTimeout;
let pendingScroll = 'top';
let scrollRatio = null;
let lastScrollRatio = 0;
let swipeGesture = null;
let suppressSwipeClick = false;
let suppressSwipeClickTimeout;

export function showError(error) {
    loader(false);
    if (error instanceof HttpError && (error.status === 401 || error.status === 403)) {
        console.log(`Authentication error`);
        location.reload();
        return;
    }
    alert(`Ошибка: ${error}`);
}

/**
 * Show loader
 * @param {Boolean} on 
 */
function loader(on) {
    if (on) {
        eLoader.classList.add('loader-on');
    } else {
        eLoader.classList.remove('loader-on');
    }
}

/**
 * Show page
 * 
 * @param {Number} page page
 */
function renderWidthPercent() {
    // лупа и режим "вписать по ширине" показывают тот же рендер, что и режим ширины 100%
    return magnifier || fitWidth ? 100 : viewportWidthPercent;
}

function showPage(page) {
    const width = Math.round(window.innerWidth * renderWidthPercent() / 100);
    const height = window.innerHeight;
    const url = `${baseBookUrl}/render/${page}?deviceUuid=${deviceUuid}&width=${width}&height=${height}`;
    loader(true);
    ajax(url, { json: false }).then(data => {
        const image = new Image();
        image.onload = () => {
            eView.src = url;
            currentPage = page;
            if (ePage) ePage.innerHTML = `${currentPage}/${pages}`;
            savePosition(page)
            loader(false);
            applyPendingScroll();
            requestAnimationFrame(syncMagnifier);
        }
        image.src = url;
    })
    .catch(error => {
        showError(error);
    })
}

/**
 * Convert a server position to a page number.
 * If positionByPage is empty, the page number equals the position.
 *
 * @param {Number} position server position
 * @returns {Number} page number
 */
function positionToPage(position) {
    if (!positionByPage || Object.keys(positionByPage).length === 0) {
        return position;
    }
    let result = 1;
    let best = -1;
    for (const [page, pagePosition] of Object.entries(positionByPage)) {
        if (pagePosition <= position && pagePosition > best) {
            best = pagePosition;
            result = Number(page);
        }
    }
    return result;
}

/**
 * Convert a page number to a server position.
 * If positionByPage is empty, the position equals the page number.
 *
 * @param {Number} page page number
 * @returns {Number} server position
 */
function pageToPosition(page) {
    if (!positionByPage || Object.keys(positionByPage).length === 0) {
        return page;
    }
    return positionByPage[page];
}

function loadPosition() {
    ajax(`${baseBookmarkUrl}`)
        .then(data => {
            showPage(positionToPage(data));
        })
        .catch(error => showError(error));
}

function savePosition(page) {
    const position = pageToPosition(page);
    ajax(`${baseBookmarkUrl}&position=${position}`, { method: 'PUT', json: false })
        .catch(error => showError(error))
}

function loadInfo() {
    const width = Math.round(window.innerWidth * renderWidthPercent() / 100);
    const height = window.innerHeight;
    const url = `${baseBookUrl}?deviceUuid=${deviceUuid}&width=${width}&height=${height}`;
    ajax(url).then(data => {
            pages = data.pages;
            if (eTitle) eTitle.innerHTML = data.title;
            if (eAuthor) eAuthor.innerHTML = data.author;
            positionByPage = data.positionByPage;
            if (onFocusNotInit) {
                onFocusNotInit = false
                window.addEventListener('focus', () => loadPosition());
            }
            loadPosition();
        })
        .catch(error => showError(error));
}

function flashState(on) {
    let elements = document.querySelectorAll('.overlay');
    for (let e of elements) {
        if (on) {
            e.classList.add('flash-on');
        } else {
            e.classList.remove('flash-on');
        }
    }
}

function flash() {
    if (!flashTimeout) {
        clearTimeout(flashTimeout);
    }
    flashTimeout = setTimeout(
        () => {
            flashState(false);
        },
        500
    )
    flashState(true);
}

function showMenu() {
    if (menuTimeout) {
        clearTimeout(menuTimeout);
    }
    eMenu.classList.remove('menu-hidden');
    eMenu.classList.add('menu-visible');
    menuTimeout = setTimeout(() => {
        hideMenu();
    }, 3000);
}

function hideMenu() {
    if (menuTimeout) {
        clearTimeout(menuTimeout);
        menuTimeout = null;
    }
    eMenu.classList.remove('menu-visible');
    eMenu.classList.add('menu-hidden');
}

function toggleMenu() {
    if (eMenu.classList.contains('menu-visible')) {
        hideMenu();
    } else {
        showMenu();
    }
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}

function saveMode() {
    localStorage.setItem(VIEWPORT_WIDTH_KEY, String(viewportWidthPercent));
    localStorage.setItem(MAGNIFIER_KEY, magnifier ? 'true' : 'false');
    localStorage.setItem(FIT_WIDTH_KEY, fitWidth ? 'true' : 'false');
}

function isFitWidthAvailable() {
    return bookType === 'PDF';
}

function updateMode() {
    const fitWidthOn = fitWidth && isFitWidthAvailable();
    eBook.classList.toggle('magnifier-mode', magnifier);
    eBook.classList.toggle('fit-width', fitWidthOn);
    eMagnifier.classList.remove('magnifier-visible');
    eMagnifierFrame.classList.remove('magnifier-frame-visible');
    updateMenuVisibility();
    pendingScroll = 'top';
    // после смены режима возвращаемся к началу страницы
    requestAnimationFrame(() => {
        if (eScroll) eScroll.scrollTop = 0;
    });
}

/**
 * Прокручивает страницу в начало или в конец после загрузки изображения.
 */
function applyPendingScroll() {
    if (!fitWidth || !eScroll) {
        pendingScroll = 'top';
        scrollRatio = null;
        return;
    }
    const target = pendingScroll;
    pendingScroll = 'top';
    const apply = () => {
        if (scrollRatio !== null) {
            // после изменения размера окна сохраняем относительную позицию
            eScroll.scrollTop = Math.round(scrollRatio * fitWidthMaxScroll());
            scrollRatio = null;
        } else {
            eScroll.scrollTop = target === 'bottom' ? eScroll.scrollHeight : 0;
        }
    };
    if (eView.decode) {
        eView.decode().then(() => requestAnimationFrame(apply), () => requestAnimationFrame(apply));
    } else {
        requestAnimationFrame(apply);
    }
}

function setViewportWidth(percent) {
    magnifier = false;
    fitWidth = false;
    magnifierPoint = null;
    magnifierDrag = null;
    suppressMagnifierClick = false;
    viewportWidthPercent = percent;
    saveMode();
    updateMode();
    loadInfo();
}

function setMagnifier(on) {
    magnifier = on;
    if (on) {
        fitWidth = false;
    }
    magnifierPoint = null;
    magnifierDrag = null;
    suppressMagnifierClick = false;
    saveMode();
    updateMode();
    loadInfo();
}

function setFitWidth(on) {
    fitWidth = on && isFitWidthAvailable();
    if (fitWidth) {
        magnifier = false;
        magnifierPoint = null;
        magnifierDrag = null;
        suppressMagnifierClick = false;
    }
    saveMode();
    updateMode();
    loadInfo();
}

function loadDisplayMode() {
    const saved = localStorage.getItem(VIEWPORT_WIDTH_KEY);
    if (saved !== null) {
        const parsed = parseInt(saved, 10);
        if (!isNaN(parsed) && (parsed === 50 || parsed === 100)) {
            viewportWidthPercent = parsed;
        }
    }
    magnifier = localStorage.getItem(MAGNIFIER_KEY) === 'true';
    fitWidth = localStorage.getItem(FIT_WIDTH_KEY) === 'true' && isFitWidthAvailable();
    if (fitWidth) {
        magnifier = false;
    }
}

function updateMenuVisibility() {
    if (eMenuMagnifier) {
        eMenuMagnifier.textContent = magnifier ? 'Выключить лупу' : 'Чтение с лупой';
    }
    if (eMenuFitWidth) {
        eMenuFitWidth.style.display = isFitWidthAvailable() ? '' : 'none';
        eMenuFitWidth.textContent = fitWidth ? 'Вписать страницу' : 'Вписать по ширине';
    }
    if (eMenuWidth100) {
        eMenuWidth100.style.display = viewportWidthPercent === 100 ? 'none' : '';
    }
    if (eMenuWidth50) {
        eMenuWidth50.style.display = viewportWidthPercent === 50 ? 'none' : '';
    }
}

/**
 * Обновляет положение увеличенного фрагмента в нижней панели.
 */
function updateMagnifier() {
    if (!magnifier || !magnifierPoint) {
        eMagnifier.classList.remove('magnifier-visible');
        eMagnifierFrame.classList.remove('magnifier-frame-visible');
        return;
    }
    const image = eView.getBoundingClientRect();
    if (image.width === 0 || image.height === 0) {
        return;
    }
    const book = eBook.getBoundingClientRect();
    const panelWidth = eBook.clientWidth;
    const panelHeight = eBook.clientHeight / 3;
    const width = image.width * MAGNIFIER_ZOOM;
    const height = image.height * MAGNIFIER_ZOOM;
    // точка нажатия на странице должна оказаться в центре нижней панели,
    // но у краёв страницы не показываем пустоту за её пределами
    const left = clamp(panelWidth / 2 - magnifierPoint.x * width, Math.min(panelWidth - width, 0), 0);
    const top = clamp(panelHeight / 2 - magnifierPoint.y * height, Math.min(panelHeight - height, 0), 0);
    eMagnifierView.style.width = `${width}px`;
    eMagnifierView.style.height = `${height}px`;
    eMagnifierView.style.left = `${left}px`;
    eMagnifierView.style.top = `${top}px`;
    eMagnifier.classList.add('magnifier-visible');

    // рамка на странице показывает, какая её часть видна в нижней панели:
    // переводим границы панели обратно в координаты страницы (делим на увеличение)
    const frameStartX = Math.max(-left, 0);
    const frameStartY = Math.max(-top, 0);
    const frameEndX = Math.min(-left + panelWidth, width);
    const frameEndY = Math.min(-top + panelHeight, height);
    const frameWidth = frameEndX - frameStartX;
    const frameHeight = frameEndY - frameStartY;
    if (frameWidth <= 0 || frameHeight <= 0) {
        eMagnifierFrame.classList.remove('magnifier-frame-visible');
        return;
    }
    eMagnifierFrame.style.left = `${image.left - book.left + frameStartX / MAGNIFIER_ZOOM}px`;
    eMagnifierFrame.style.top = `${image.top - book.top + frameStartY / MAGNIFIER_ZOOM}px`;
    eMagnifierFrame.style.width = `${frameWidth / MAGNIFIER_ZOOM}px`;
    eMagnifierFrame.style.height = `${frameHeight / MAGNIFIER_ZOOM}px`;
    eMagnifierFrame.classList.add('magnifier-frame-visible');
}

/**
 * Синхронизирует увеличенный фрагмент с текущей страницей.
 */
function syncMagnifier() {
    if (!magnifier || !magnifierPoint) {
        return;
    }
    if (eMagnifierView.src !== eView.src) {
        eMagnifierView.src = eView.src;
    }
    updateMagnifier();
}

/**
 * Определяет действие для нажатия в режиме лупы.
 *
 * @param {PointerEvent} event событие указателя
 * @returns {String|null} 'point' — задать точку, 'pan' — сместить фрагмент, null — без лупы
 */
function magnifierGesture(event) {
    if (!magnifier) {
        return null;
    }
    if (event.target.closest('#menu, #menu-trigger')) {
        return null;
    }
    const rect = eBook.getBoundingClientRect();
    if (event.clientY < rect.top + rect.height * 2 / 3) {
        return 'point';
    }
    // нижняя треть двигает уже показанный фрагмент
    return magnifierPoint ? 'pan' : null;
}

/**
 * Показывает увеличенный фрагмент вокруг точки нажатия.
 *
 * @param {PointerEvent} event событие указателя
 */
function showMagnifierAt(event) {
    const image = eView.getBoundingClientRect();
    if (image.width === 0 || image.height === 0) {
        return;
    }
    magnifierPoint = {
        x: clamp((event.clientX - image.left) / image.width, 0, 1),
        y: clamp((event.clientY - image.top) / image.height, 0, 1)
    };
    eMagnifierView.src = eView.src;
    updateMagnifier();
}

function onMagnifierDown(event) {
    if (event.pointerType === 'mouse' && event.button !== 0) {
        return;
    }
    const gesture = magnifierGesture(event);
    if (!gesture) {
        return;
    }
    magnifierDrag = {
        id: event.pointerId,
        mode: gesture,
        startX: event.clientX,
        startY: event.clientY,
        startPoint: magnifierPoint ? { ...magnifierPoint } : null,
        // если панорамирование началось у края страницы,
        // повторная протяжка за этот край листает дальше
        edge: gesture === 'pan' && magnifierPoint
            ? (magnifierPoint.y <= 0.02 ? 'top' : magnifierPoint.y >= 0.98 ? 'bottom' : null)
            : null,
        dragged: false
    };
    if (gesture === 'point') {
        showMagnifierAt(event);
        event.preventDefault();
    }
    document.addEventListener('pointermove', onMagnifierMove);
    document.addEventListener('pointerup', onMagnifierUp);
    document.addEventListener('pointercancel', onMagnifierUp);
}

function onMagnifierMove(event) {
    if (!magnifierDrag || event.pointerId !== magnifierDrag.id) {
        return;
    }
    if (magnifierDrag.mode === 'point') {
        showMagnifierAt(event);
        event.preventDefault();
        return;
    }
    const dx = event.clientX - magnifierDrag.startX;
    const dy = event.clientY - magnifierDrag.startY;
    if (!magnifierDrag.dragged && Math.hypot(dx, dy) < MAGNIFIER_DRAG_THRESHOLD) {
        return;
    }
    magnifierDrag.dragged = true;
    const image = eView.getBoundingClientRect();
    if (image.width === 0 || image.height === 0) {
        return;
    }
    // фрагмент в нижней панели двигается вслед за пальцем
    magnifierPoint = {
        x: clamp(magnifierDrag.startPoint.x - dx / (image.width * MAGNIFIER_ZOOM), 0, 1),
        y: clamp(magnifierDrag.startPoint.y - dy / (image.height * MAGNIFIER_ZOOM), 0, 1)
    };
    updateMagnifier();
    event.preventDefault();
}

function onMagnifierUp(event) {
    if (!magnifierDrag || event.pointerId !== magnifierDrag.id) {
        return;
    }
    if (magnifierDrag.mode === 'pan' && magnifierDrag.dragged) {
        const dy = event.clientY - magnifierDrag.startY;
        const dx = event.clientX - magnifierDrag.startX;
        const vertical = Math.abs(dy) >= Math.abs(dx);
        // в лупе страница не скроллится: у края панорамирования
        // повторная вертикальная протяжка листает страницу
        if (vertical && dy <= -PAGE_TURN_DRAG_THRESHOLD && magnifierDrag.edge === 'bottom') {
            next();
        } else if (vertical && dy >= PAGE_TURN_DRAG_THRESHOLD && magnifierDrag.edge === 'top') {
            prev();
        }
        // после панорамирования не даём клику по нижней трети перелистнуть страницу
        suppressMagnifierClick = true;
        clearTimeout(suppressMagnifierClickTimeout);
        suppressMagnifierClickTimeout = setTimeout(() => {
            suppressMagnifierClick = false;
        }, MAGNIFIER_CLICK_SUPPRESS_TIMEOUT);
    }
    magnifierDrag = null;
    document.removeEventListener('pointermove', onMagnifierMove);
    document.removeEventListener('pointerup', onMagnifierUp);
    document.removeEventListener('pointercancel', onMagnifierUp);
}

function onMagnifierClick(event) {
    if (!suppressMagnifierClick) {
        return;
    }
    suppressMagnifierClick = false;
    clearTimeout(suppressMagnifierClickTimeout);
    event.stopPropagation();
    event.preventDefault();
}

/**
 * Величина вертикального скролла страницы в режиме "вписать по ширине".
 */
function fitWidthMaxScroll() {
    return Math.max(0, eScroll.scrollHeight - eScroll.clientHeight);
}

/**
 * Запоминает относительную позицию скролла, чтобы сохранить её при смене размера окна.
 */
function onFitWidthScroll() {
    if (!fitWidth) {
        return;
    }
    const max = fitWidthMaxScroll();
    lastScrollRatio = max > 0 ? eScroll.scrollTop / max : 0;
}

/**
 * Начинает вертикальный жест: запоминает точку старта и позицию скролла.
 * Жест работает во всех режимах: в "вписать по ширине" листает у края страницы,
 * в остальных режимах — сразу.
 */
function startSwipeGesture(x, y) {
    swipeGesture = {
        startX: x,
        startY: y,
        startScrollTop: fitWidth ? eScroll.scrollTop : 0,
        moved: false
    };
}

function trackSwipeGesture(y) {
    if (swipeGesture && Math.abs(y - swipeGesture.startY) > 8) {
        swipeGesture.moved = true;
    }
}

/**
 * Завершает жест и листает страницу, если жест достаточно длинный.
 *
 * @param {Number} x координата указателя по горизонтали
 * @param {Number} y координата указателя по вертикали
 */
function finishSwipeGesture(x, y) {
    if (!swipeGesture) {
        return;
    }
    const gesture = swipeGesture;
    swipeGesture = null;
    const dy = y - gesture.startY;
    const dx = x - gesture.startX;
    // листаем только вертикальными жестами
    const vertical = Math.abs(dy) >= Math.abs(dx);

    if (fitWidth) {
        const max = fitWidthMaxScroll();
        const atBottom = gesture.startScrollTop >= max - 1;
        const atTop = gesture.startScrollTop <= 0;
        // листаем только если жест начался у края страницы,
        // то есть это повторная протяжка, а не докрутка до конца
        if (vertical && dy <= -PAGE_TURN_DRAG_THRESHOLD && atBottom) {
            next();
        } else if (vertical && dy >= PAGE_TURN_DRAG_THRESHOLD && atTop) {
            prev();
        }
    } else if (!magnifier) {
        // в остальных режимах страница не скроллится — листаем сразу,
        // вверх — следующая, вниз — предыдущая
        if (vertical && dy <= -PAGE_TURN_DRAG_THRESHOLD) {
            next();
        } else if (vertical && dy >= PAGE_TURN_DRAG_THRESHOLD) {
            prev();
        }
    }
    if (gesture.moved) {
        suppressSwipeClick = true;
        clearTimeout(suppressSwipeClickTimeout);
        suppressSwipeClickTimeout = setTimeout(() => {
            suppressSwipeClick = false;
        }, CLICK_SUPPRESS_TIMEOUT);
    }
}

function swipeStartsOnControl(event) {
    return !!(event.target.closest && event.target.closest('#menu, #magnifier'));
}

function onSwipeTouchStart(event) {
    if (event.touches.length !== 1 || swipeStartsOnControl(event)) {
        return;
    }
    const touch = event.touches[0];
    startSwipeGesture(touch.clientX, touch.clientY);
}

function onSwipeTouchMove(event) {
    if (!swipeGesture || event.touches.length !== 1) {
        return;
    }
    const y = event.touches[0].clientY;
    trackSwipeGesture(y);
    const dy = y - swipeGesture.startY;
    if (!fitWidth) {
        // страница не скроллится: гасим системные жесты браузера (pull-to-refresh и т.п.),
        // иначе свайп вниз может обновить страницу вместо листания назад
        if (event.cancelable && Math.abs(dy) > 8) {
            event.preventDefault();
        }
        return;
    }
    const max = fitWidthMaxScroll();
    const atBottom = swipeGesture.startScrollTop >= max - 1;
    const atTop = swipeGesture.startScrollTop <= 0;
    // у края не даём странице оттягиваться: это уже жест листания
    if (event.cancelable && ((dy < 0 && atBottom) || (dy > 0 && atTop))) {
        event.preventDefault();
    }
}

function onSwipeTouchEnd(event) {
    if (!swipeGesture) {
        return;
    }
    const touch = event.changedTouches[0];
    finishSwipeGesture(touch.clientX, touch.clientY);
}

function onSwipeTouchCancel() {
    swipeGesture = null;
}

function onSwipePointerDown(event) {
    if (event.pointerType !== 'mouse' || event.button !== 0 || swipeStartsOnControl(event)) {
        return;
    }
    startSwipeGesture(event.clientX, event.clientY);
}

function onSwipePointerMove(event) {
    if (!swipeGesture || event.pointerType !== 'mouse') {
        return;
    }
    trackSwipeGesture(event.clientY);
}

function onSwipePointerUp(event) {
    if (!swipeGesture || event.pointerType !== 'mouse') {
        return;
    }
    finishSwipeGesture(event.clientX, event.clientY);
}

/**
 * Гасит клик, который браузер шлёт после протяжки, чтобы не листать дважды.
 */
function onSwipeClick(event) {
    if (!suppressSwipeClick) {
        return;
    }
    suppressSwipeClick = false;
    clearTimeout(suppressSwipeClickTimeout);
    event.stopPropagation();
    event.preventDefault();
}

function onFitWidthClick(event) {
    if (!fitWidth) {
        return;
    }
    if (fitWidthMaxScroll() > 1) {
        // страницу можно скроллить — тап не листает, чтобы не пропустить текст
        return;
    }
    const rect = eBook.getBoundingClientRect();
    if (event.clientY < rect.top + rect.height * 0.7) {
        return;
    }
    if (event.clientX > rect.left + rect.width / 2) {
        next();
    } else {
        prev();
    }
}

function next() {
    if (currentPage < pages) {
        pendingScroll = 'top';
        flash();
        showPage(currentPage + 1);
    }
}

function prev() {
    if (currentPage > 1) {
        pendingScroll = 'bottom';
        flash();
        showPage(currentPage - 1);
    }
}

function init() {
    eBook = document.getElementById('book');
    eScroll = document.getElementById('scroll');
    eView = document.getElementById('view');
    eAuthor = document.getElementById('author');
    ePage = document.getElementById('page');
    eTitle = document.getElementById('title');
    eLoader = document.getElementById('loader');
    eMenu = document.getElementById('menu');
    eMenuTrigger = document.getElementById('menu-trigger');
    eMenuWidth100 = document.getElementById('menu-width-100');
    eMenuWidth50 = document.getElementById('menu-width-50');
    eMenuFitWidth = document.getElementById('menu-fit-width');
    eMenuMagnifier = document.getElementById('menu-magnifier');
    eMagnifier = document.getElementById('magnifier');
    eMagnifierView = document.getElementById('magnifier-view');
    eMagnifierFrame = document.getElementById('magnifier-frame');
    bookType = eBook.getAttribute('data-scope');
    eView.draggable = false;
    const id = eBook.getAttribute('data-id');
    baseBookUrl = `/api/v1/book/${id}`
    baseBookmarkUrl = `/api/v1/bookmark?bookId=${id}`;
    document.getElementById('next').onclick = () => next();
    document.getElementById('prev').onclick = () => prev();
    eMenuTrigger.onclick = () => toggleMenu();
    const eMenuSettings = document.getElementById('menu-settings');
    if (eMenuSettings) eMenuSettings.onclick = () => {
        const scope = eBook.getAttribute('data-scope') || 'GLOBAL';
        window.location.href = `/settings?scope=${scope}`;
    };
    if (eMenuWidth100) eMenuWidth100.onclick = () => setViewportWidth(100);
    if (eMenuWidth50) eMenuWidth50.onclick = () => setViewportWidth(50);
    if (eMenuFitWidth) eMenuFitWidth.onclick = () => setFitWidth(!fitWidth);
    if (eMenuMagnifier) eMenuMagnifier.onclick = () => setMagnifier(!magnifier);
    eBook.addEventListener('pointerdown', onMagnifierDown);
    eBook.addEventListener('click', onMagnifierClick, true);
    eBook.addEventListener('touchstart', onSwipeTouchStart, { passive: true });
    eBook.addEventListener('touchmove', onSwipeTouchMove, { passive: false });
    eBook.addEventListener('touchend', onSwipeTouchEnd);
    eBook.addEventListener('touchcancel', onSwipeTouchCancel);
    eBook.addEventListener('pointerdown', onSwipePointerDown);
    eBook.addEventListener('click', onSwipeClick, true);
    eScroll.addEventListener('click', onFitWidthClick);
    eScroll.addEventListener('scroll', onFitWidthScroll);
    document.addEventListener('pointermove', onSwipePointerMove);
    document.addEventListener('pointerup', onSwipePointerUp);
    document.addEventListener('pointercancel', onSwipePointerUp);
    loadDisplayMode();
    updateMode();
    document.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
            e.preventDefault();
            next();
        } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
            e.preventDefault();
            prev();
        }
    });
    window.addEventListener('resize', debounce(() => {
        if (fitWidth) {
            // при изменении размера окна восстанавливаем ту же относительную позицию
            scrollRatio = lastScrollRatio;
        }
        loadInfo();
    }, 300));
    window.addEventListener('resize', debounce(() => updateMagnifier(), 100));
    loadInfo();
}

document.addEventListener("DOMContentLoaded", init);