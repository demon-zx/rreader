import { ajax, debounce, showError } from "./common.js";

let baseBookUrl;
let baseBookmarkUrl;
let eView;
let currentPage = 1;
let pages = 0;

/**
 * Show page
 * 
 * @param {Number} page page
 */
function showPage(page) {
    const url = `${baseBookUrl}/render/${page}`;
    const image = new Image();
    image.onload = () => {
        eView.src = url;
        currentPage = page;
        savePosition(page)
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
            loadPosition();
        })
        .catch(error => showError(error));
}

function next() {
    if (currentPage < pages) {
        showPage(currentPage + 1)
    }
}

//TODO spinner for load
//TODO prev page
//TODO flash controls
//TODO pre caching
//TODO menu
//TODO zoom

function init() {
    eView = document.getElementById('view');
    const id = document.getElementById('book').getAttribute('data-id');
    baseBookUrl = `/api/v1/book/${id}`
    baseBookmarkUrl = `/api/v1/bookmark?bookId=${id}`;
    document.getElementById('next').onclick = () => next();
    loadInfo()
}

document.addEventListener("DOMContentLoaded", init);