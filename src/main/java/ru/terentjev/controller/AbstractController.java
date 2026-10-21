package ru.terentjev.controller;

import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ru.terentjev.exception.InvalidArgumentException;

import java.util.function.Function;

public abstract class AbstractController {
    protected final Logger log;

    protected AbstractController(Logger log) {
        this.log = log;
    }

    protected void run(
            FailableRunnable<Exception> call
    ) throws ResponseStatusException {
        run(
                () -> {
                    call.run();
                    return null;
                },
                o -> o
        );
    }

    protected <T, O> O run(
            FailableSupplier<T, Exception> call,
            Function<T, O> converter
    ) throws ResponseStatusException {
        try {
            return converter.apply(call.get());
        } catch (InvalidArgumentException e) {
            log.error("Некорректные аргументы", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ошибка на сервере", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка на сервере", e);
        }
    }
}
