import { ajax, debounce } from "./common.js";

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


export function showError(message) {
    loader(false);
    alert(`Ошибка: ${message}`);
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
    const url = `${baseBookUrl}/render/${page}`;
    loader(true);
    const image = new Image();
    image.onload = () => {
        eView.src = url;
        currentPage = page;
        if (ePage) ePage.innerHTML = `${currentPage}/${pages}`;
        savePosition(page)
        loader(false);
    }
    image.onerror = (err) => {
        showError(err);
    }
    image.src = url;
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
    ajax(baseBookUrl)
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