import {ajax, debounce, showError, getDeviceUuid } from "./common.js";

const deviceUuid = getDeviceUuid();
const PAGE_SIZE = 10;

let eBooks;
let eRecent;
let eRecentBooks;
let eSearch;
const ePagers = [];

let page = 0;
let all = 0;
let allPages = 0;
let loadSeq = 0;

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

/**
 * Возвращает номера страниц для пагинатора: первая, последняя и соседние
 * с текущей, остальные заменяются многоточием.
 *
 * @param {number} current текущая страница, начиная с 1
 * @param {number} total всего страниц
 * @returns {Array<number|string>}
 */
function pageNumbers(current, total) {
    const result = [];
    for (let p = 1; p <= total; p++) {
        if (p === 1 || p === total || Math.abs(p - current) <= 1) {
            result.push(p);
        } else if (result[result.length - 1] !== '...') {
            result.push('...');
        }
    }
    return result;
}

function pagerButton(label, targetPage, options = {}) {
    const {active = false, disabled = false, title = ''} = options;
    const classes = ['page-item'];
    if (active) {
        classes.push('active');
    }
    if (disabled) {
        classes.push('disabled');
    }
    const ariaCurrent = active ? ' aria-current="page"' : '';
    return `<li class="${classes.join(' ')}">`
            + `<button type="button" class="page-link" data-page="${targetPage}"`
            + `${disabled ? ' disabled' : ''}${ariaCurrent} title="${title}">${label}</button>`
            + `</li>`;
}

function renderPager() {
    if (allPages <= 1) {
        ePagers.forEach(pager => {
            pager.element.style.display = 'none';
            pager.pages.innerHTML = '';
        });
        return;
    }
    const from = page * PAGE_SIZE + 1;
    const to = Math.min(all, (page + 1) * PAGE_SIZE);
    const info = `${from}–${to} из ${all}`;

    const items = [
        pagerButton('«', page - 1, {
            disabled: page === 0,
            title: 'Предыдущая страница'
        })
    ];
    pageNumbers(page + 1, allPages).forEach(p => {
        if (p === '...') {
            items.push('<li class="page-item disabled"><span class="page-link">…</span></li>');
        } else {
            items.push(pagerButton(String(p), p - 1, {
                active: p === page + 1,
                title: `Страница ${p}`
            }));
        }
    });
    items.push(
            pagerButton('»', page + 1, {
                disabled: page >= allPages - 1,
                title: 'Следующая страница'
            })
    );
    const html = items.join('');
    ePagers.forEach(pager => {
        pager.element.style.display = '';
        pager.info.textContent = info;
        pager.pages.innerHTML = html;
    });
}

function renderBooks(books) {
    if (!books.length) {
        eBooks.innerHTML = '<div class="col-12 py-3 text-body-secondary">Книги не найдены</div>';
        return;
    }
    eBooks.innerHTML = books
            .map(book => bookCard(book))
            .join('');
}

function updateUrl() {
    const params = new URLSearchParams();
    const q = eSearch.value.trim();
    if (q) {
        params.set('q', q);
    }
    if (page > 0) {
        params.set('page', String(page + 1));
    }
    const query = params.toString();
    const url = query ? `${window.location.pathname}?${query}` : window.location.pathname;
    window.history.replaceState(null, '', url);
}

function loadBooks() {
    const seq = ++loadSeq;
    const q = eSearch.value.trim();
    ajax(`/api/v1/library?page=${page}&pageSize=${PAGE_SIZE}&q=${encodeURIComponent(q)}`)
            .then(data => {
                if (seq !== loadSeq) {
                    return;
                }
                all = data.all;
                allPages = data.allPages;
                const lastPage = Math.max(0, allPages - 1);
                if (page > lastPage) {
                    page = lastPage;
                    updateUrl();
                    loadBooks();
                    return;
                }
                renderBooks(data.items);
                renderPager();
                updateUrl();
            })
            .catch(error => {
                if (seq === loadSeq) {
                    showError(error.message);
                }
            })
}

function onPagerClick(event) {
    const button = event.target.closest('button[data-page]');
    if (!button || button.disabled) {
        return;
    }
    goToPage(Number(button.dataset.page));
}

function goToPage(targetPage) {
    if (targetPage < 0 || targetPage >= allPages || targetPage === page) {
        return;
    }
    page = targetPage;
    loadBooks();
    eBooks.scrollIntoView({behavior: 'smooth', block: 'start'});
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

function initState() {
    const params = new URLSearchParams(window.location.search);
    eSearch.value = params.get('q') || '';
    const urlPage = Number.parseInt(params.get('page'), 10);
    page = Number.isNaN(urlPage) || urlPage < 1 ? 0 : urlPage - 1;
}

function initSearch() {
    const lb = debounce(() => {
        page = 0;
        loadBooks();
    }, 300)
    eSearch.addEventListener('input', lb)
    const eSearchClear = document.getElementById('search-clear');
    eSearchClear.addEventListener('click', () => {
        eSearch.value = '';
        page = 0;
        loadBooks();
    })

}

function init() {
    eBooks = document.getElementById('books');
    eRecent = document.getElementById('recent');
    eRecentBooks = document.getElementById('recent-books');
    eSearch = document.getElementById('search');

    document.querySelectorAll('.pager').forEach(element => {
        const pager = {
            element,
            info: element.querySelector('.pager-info'),
            pages: element.querySelector('.pager-pages')
        };
        pager.pages.addEventListener('click', onPagerClick);
        ePagers.push(pager);
    });

    initState();
    loadBooks();
    loadRecent();
    initSearch();
}

document.addEventListener("DOMContentLoaded", init);
