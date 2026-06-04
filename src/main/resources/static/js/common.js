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
                        throw new Error(`HTTP ${response.status}: ${response.statusText}. ${errorText}`);
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