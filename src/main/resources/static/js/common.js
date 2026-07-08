/**
 * Выполняет AJAX-запрос с использованием Fetch API
 * @param {string} url - адрес запроса
 * @param {Object} options - параметры запроса
 * @param {string} options.method - HTTP метод (GET, POST, PUT, DELETE и т.д.)
 * @param {Object} options.headers - объект с заголовками
 * @param {*} options.body - тело запроса (будет преобразовано в JSON, если не FormData)
 * @param {boolean} options.json - автоматически парсить ответ как JSON (по умолчанию true)
 * @returns {Promise} - промис с данными ответа
 */
export function ajax(url, options = {}) {
    const {
        method = 'GET',
        headers = {},
        body = null,
        json = true
    } = options;
    // Подготавливаем заголовки
    const requestHeaders = new Headers(headers);

    let eCsrfValue = document.querySelector('meta[name="_csrf"]');
    let eCsrfHeader = document.querySelector('meta[name="_csrf_header"]');
    if (eCsrfValue && eCsrfHeader) {
        requestHeaders.set(eCsrfHeader.content, eCsrfValue.content)
    }

    // Автоматически добавляем Content-Type: application/json если тело - объект
    if (body && typeof body === 'object' && !(body instanceof FormData)) {
        if (!requestHeaders.has('Content-Type')) {
            requestHeaders.set('Content-Type', 'application/json');
        }
    }

    // Настройки запроса
    const fetchOptions = {
        method,
        headers: requestHeaders,
    };

    // Добавляем тело, если оно есть
    if (body) {
        if (body instanceof FormData) {
            fetchOptions.body = body;
        } else if (typeof body === 'object') {
            fetchOptions.body = JSON.stringify(body);
        } else {
            fetchOptions.body = body;
        }
    }

    // Выполняем запрос
    return fetch(url, fetchOptions)
            .then(response => {
                // Если ответ не успешный (статус вне диапазона 200-299)
                if (!response.ok) {
                    // Пытаемся получить текст ошибки
                    return response.text().then(errorText => {
                        throw new HttpError(response.status, `HTTP ${response.status}: ${response.statusText}. ${errorText}`);
                    });
                }

                // Парсим ответ в зависимости от опции json
                if (json) {
                    return response.json();
                } else {
                    return response.text();
                }
            })
            .catch(error => {
                console.error('AJAX request failed:', error);
                throw error; // Пробрасываем дальше для обработки в вызывающем коде
            });
}

/**
 * Возвращает уникальный идентификатор браузера (устройства).
 * Генерируется один раз и сохраняется в localStorage.
 *
 * @returns {string} deviceUuid
 */
export function getDeviceUuid() {
    let id = localStorage.getItem('deviceUuid');
    if (!id) {
        id = crypto.randomUUID();
        localStorage.setItem('deviceUuid', id);
    }
    return id;
}

export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
  }
}

/**
 * Создаёт "тормозящую" (debounced) функцию, которая будет вызвана не чаще одного раза
 * за указанный промежуток времени, причём только после того, как вызовы прекратятся.
 *
 * @param {Function} func - функция, вызов которой нужно отложить.
 * @param {number} delayMillis - задержка в миллисекундах.
 * @param {boolean} immediate - если true, функция выполнится сразу при первом вызове,
 *                              а затем будет игнорироваться до окончания задержки.
 * @returns {Function} - обёртка с отложенным вызовом.
 */
export function debounce(func, delayMillis, immediate = false) {
    let timeoutId;

    return function (...args) {
        const context = this;

        const later = () => {
            timeoutId = null;
            if (!immediate) {
                func.apply(context, args);
            }
        };

        const callNow = immediate && !timeoutId;
        clearTimeout(timeoutId);
        timeoutId = setTimeout(later, delayMillis);

        if (callNow) {
            func.apply(context, args);
        }
    };
}

export function showError(message) {
    alert(`Ошибка: ${message}`);
}