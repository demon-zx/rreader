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


export function showError(error) {
    loader(false);
    if (error instanceof HttpError && error.status === 401) {
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
    const url = `${baseBookUrl}/render/${page}?deviceUuid=${deviceUuid}`;
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
    // image.onload = () => {
    //     eView.src = url;
    //     currentPage = page;
    //     if (ePage) ePage.innerHTML = `${currentPage}/${pages}`;
    //     savePosition(page)
    //     loader(false);
    // }
    // image.onerror = (err) => {
    //     showError(err);
    // }
}

function loadPosition() {
    ajax(`${baseBookmarkUrl}`)
        .then(data => {
            showPage(data);
        })
        .catch(error => showError(error));
}

function savePosition(page) {
    ajax(`${baseBookmarkUrl}&position=${page}`, { method: 'PUT', json: false })
        .catch(error => showError(error))
}

function loadInfo() {
    const width = window.innerWidth;
    const height = window.innerHeight;
    const url = `${baseBookUrl}?deviceUuid=${deviceUuid}&width=${width}&height=${height}`;
    ajax(url)
        .then(data => {
            pages = data.pages;
            if (eTitle) eTitle.innerHTML = data.title;
            if (eAuthor) eAuthor.innerHTML = data.author;
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
    loadInfo();
}

document.addEventListener("DOMContentLoaded", init);