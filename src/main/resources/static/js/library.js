import {ajax} from "./common.js";

ajax("http://localhost:7070/api/v1/library?from=0&limit=10")
        .then(data => {
            console.log('Пост:', data);
        })
        .catch(error => {
            console.error('Ошибка:', error.message);
        })