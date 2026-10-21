package ru.terentjev.exception;

/**
 * Базовое сервисное исключение
 */
public abstract class ServiceException extends Exception {
    public ServiceException(String message) {
        super(message);
    }
}
