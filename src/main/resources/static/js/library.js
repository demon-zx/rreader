import {ajax, debounce, showError, getDeviceUuid } from "./common.js";

const deviceUuid = getDeviceUuid();

let eBooks;
let eRecent;
let eRecentBooks;
let eSearch;

let page = 0;
let pageSize = 10;

function bookCard(book, percent) {
    let sequence = '';
    if (book.sequenceName) {
        let number = book.sequenceNumber != null ? ` #${book.sequenceNumber}` : '';
        sequence = ` <span class="badge text-bg-info book-sequence">${book.sequenceName}${number}</span>`;
    }
    let percentBadge = '';
    if (percent != null) {
        percentBadge = `<span class="badge text-bg-dark read-percent">${percent}%</span>`;
    }
    return `
        <div class="col-12 col-lg-6">
            <div class="card mb-1" onclick="location.href='/view?id=${book.id}'">
                <div class="row g-0">
                    <div class="col-auto book-cover-wrap">
                        <img src="/api/v1/book/${book.id}/cover?deviceUuid=${deviceUuid}" class="book-cover"
                             alt="...">
                        ${percentBadge}
                    </div>
                    <div class="col">
                        <div class="card-body">
                            <h5 class="card-title book-author">${book.author}</h5>
                            <p class="card-text book-title" title="${book.title}">${book.title}</p>
                            <h6>Размер: <span class="badge text-bg-secondary">${book.size}</span>${sequence}</h6>
                        </div>
                    </div>
                </div>
            </div>
        </div>
`
}

function loadBooks() {
    ajax(`/api/v1/library?page=${page}&pageSize=${pageSize}&q=${eSearch.value}`)
            .then(data => {
                let html = '';
                data.items.forEach(i => {
                    html += bookCard(i)
                })
                eBooks.innerHTML = html;
            })
            .catch(error => {
                showError(error.message);
            })
}

function loadRecent() {
    const width = Math.round(window.innerWidth);
    const height = window.innerHeight;
    ajax(`/api/v1/library/recent?deviceUuid=${deviceUuid}&width=${width}&height=${height}&limit=4`)
            .then(data => {
                let html = '';
                data.forEach(i => {
                    html += bookCard(i, i.percent)
                })
                eRecentBooks.innerHTML = html;
                eRecent.style.display = data.length ? '' : 'none';
            })
            .catch(error => {
                showError(error.message);
            })
}

function initSearch() {
    const lb = debounce(loadBooks, 300)
    eSearch.addEventListener('input', lb)
    const eSearchClear = document.getElementById('search-clear');
    eSearchClear.addEventListener('click', () => {
        eSearch.value = '';
        loadBooks();
    })

}

function init() {
    eBooks = document.getElementById('books');
    eRecent = document.getElementById('recent');
    eRecentBooks = document.getElementById('recent-books');
    eSearch = document.getElementById('search');

    loadBooks();
    loadRecent();
    initSearch();
}

document.addEventListener("DOMContentLoaded", init);