import {ajax} from "./common.js";
import {debounce} from "./common.js";

const eBooks = document.getElementById('books');
const eSearch = document.getElementById('search');

let page = 0;
let pageSize = 10;

function showError(message) {
    alert(`Ошибка: ${message}`);
}

function loadBooks() {
    ajax(`/api/v1/library?page=${page}&pageSize=${pageSize}&q=${eSearch.value}`)
            .then(data => {
                let html = '';
                data.items.forEach(i => {
                    html += `
                    <div class="col-12 col-lg-6">
                        <div class="card mb-1" onclick="location.href='/view?id=${i.id}'">
                            <div class="row g-0">
                                <div class="col-auto">
                                    <img src="/api/v1/book/cover?id=${i.id}" class="book-cover"
                                         alt="...">
                                </div>
                                <div class="col">
                                    <div class="card-body">
                                        <h5 class="card-title book-author">${i.author}</h5>
                                        <p class="card-text book-title" title="${i.title}">${i.title}</p>
                                        <h6>Размер: <span class="badge text-bg-secondary">${i.size}</span></h6>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
`
                })
                eBooks.innerHTML = html;
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
    loadBooks();
    initSearch();
}

document.addEventListener("DOMContentLoaded", init);