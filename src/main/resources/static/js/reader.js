import { ajax, debounce, HttpError, getDeviceUuid } from "./common.js";

const deviceUuid = getDeviceUuid();

const VIEWPORT_WIDTH_KEY = 'rreader_viewport_width';
const MAGNIFIER_KEY = 'rreader_magnifier';
const MAGNIFIER_ZOOM = 2;

let eBook;
let eMenu;
let eMenuTrigger;
let eMenuWidth100;
let eMenuWidth50;
let eMenuMagnifier;
let eMagnifier;
let eMagnifierView;
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
let magnifierPoint = null;
let magnifierPointerId = null;

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
    // лупа показывает тот же рендер, что и режим ширины 100%
    return magnifier ? 100 : viewportWidthPercent;
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
}

function updateMode() {
    eBook.classList.toggle('magnifier-mode', magnifier);
    eMagnifier.classList.remove('magnifier-visible');
    updateMenuVisibility();
}

function setViewportWidth(percent) {
    magnifier = false;
    magnifierPoint = null;
    magnifierPointerId = null;
    viewportWidthPercent = percent;
    saveMode();
    updateMode();
    loadInfo();
}

function setMagnifier(on) {
    magnifier = on;
    magnifierPoint = null;
    magnifierPointerId = null;
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
}

function updateMenuVisibility() {
    if (eMenuMagnifier) {
        eMenuMagnifier.textContent = magnifier ? 'Выключить лупу' : 'Чтение с лупой';
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
        return;
    }
    const image = eView.getBoundingClientRect();
    if (image.width === 0 || image.height === 0) {
        return;
    }
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
 * Проверяет, что нажатие пришлось на область страницы в режиме лупы.
 *
 * @param {PointerEvent} event событие указателя
 * @returns {Boolean} true, если нужно показать/сместить лупу
 */
function isMagnifierPoint(event) {
    if (!magnifier) {
        return false;
    }
    if (event.target.closest('#menu, #menu-trigger')) {
        return false;
    }
    const rect = eBook.getBoundingClientRect();
    return event.clientY < rect.top + rect.height * 2 / 3;
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
    if (!isMagnifierPoint(event)) {
        return;
    }
    magnifierPointerId = event.pointerId;
    showMagnifierAt(event);
    document.addEventListener('pointermove', onMagnifierMove);
    document.addEventListener('pointerup', onMagnifierUp);
    document.addEventListener('pointercancel', onMagnifierUp);
    event.preventDefault();
}

function onMagnifierMove(event) {
    if (event.pointerId !== magnifierPointerId) {
        return;
    }
    showMagnifierAt(event);
    event.preventDefault();
}

function onMagnifierUp(event) {
    if (event.pointerId !== magnifierPointerId) {
        return;
    }
    magnifierPointerId = null;
    document.removeEventListener('pointermove', onMagnifierMove);
    document.removeEventListener('pointerup', onMagnifierUp);
    document.removeEventListener('pointercancel', onMagnifierUp);
}

function next() {
    if (currentPage < pages) {
        flash();
        showPage(currentPage + 1);
    }
}

function prev() {
    if (currentPage > 1) {
        flash();
        showPage(currentPage - 1);
    }
}

function init() {
    eBook = document.getElementById('book');
    eView = document.getElementById('view');
    eAuthor = document.getElementById('author');
    ePage = document.getElementById('page');
    eTitle = document.getElementById('title');
    eLoader = document.getElementById('loader');
    eMenu = document.getElementById('menu');
    eMenuTrigger = document.getElementById('menu-trigger');
    eMenuWidth100 = document.getElementById('menu-width-100');
    eMenuWidth50 = document.getElementById('menu-width-50');
    eMenuMagnifier = document.getElementById('menu-magnifier');
    eMagnifier = document.getElementById('magnifier');
    eMagnifierView = document.getElementById('magnifier-view');
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
    if (eMenuMagnifier) eMenuMagnifier.onclick = () => setMagnifier(!magnifier);
    eBook.addEventListener('pointerdown', onMagnifierDown);
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
    window.addEventListener('resize', debounce(() => loadInfo(), 300));
    window.addEventListener('resize', debounce(() => updateMagnifier(), 100));
    loadInfo();
}

document.addEventListener("DOMContentLoaded", init);