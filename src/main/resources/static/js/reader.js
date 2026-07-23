import { ajax, debounce, HttpError, getDeviceUuid } from "./common.js";

const deviceUuid = getDeviceUuid();

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
function showPage(page) {
    const width = window.innerWidth;
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
    const width = window.innerWidth;
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
    eView = document.getElementById('view');
    eAuthor = document.getElementById('author');
    ePage = document.getElementById('page');
    eTitle = document.getElementById('title');
    eLoader = document.getElementById('loader');
    const id = document.getElementById('book').getAttribute('data-id');
    baseBookUrl = `/api/v1/book/${id}`
    baseBookmarkUrl = `/api/v1/bookmark?bookId=${id}`;
    document.getElementById('next').onclick = () => next();
    document.getElementById('prev').onclick = () => prev();
    window.addEventListener('resize', debounce(() => loadInfo(), 300));
    loadInfo();
}

document.addEventListener("DOMContentLoaded", init);