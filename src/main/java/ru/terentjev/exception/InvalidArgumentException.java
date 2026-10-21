package ru.terentjev.exception;

/**
 * Некорректный входной аргумент
 */
public class InvalidArgumentException extends ServiceException {
    public InvalidArgumentException(String message) {
        super(message);
    }
}
