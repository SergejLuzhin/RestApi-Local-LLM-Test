package com.skillfactory.luzhin.bookingservice.exception;

/**
 * Исключение, указывающее на сбой при взаимодействии с Hotel Service.
 *
 * <p>Выбрасывается при ошибках HTTP-ответов (4xx, 5xx), таймаутах или
 * других проблемах при вызове Hotel Management Service через Feign-клиент.
 *
 * <p>Используется для обработки ошибок в процессе двухшаговой транзакции
 * бронирования (PENDING → CANCELLED при сбое Hotel Service).
 */
public class HotelServiceException extends RuntimeException {

    /**
     * Создаёт новое исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public HotelServiceException(String message) {
        super(message);
    }

    /**
     * Создаёт новое исключение с указанным сообщением и причиной.
     *
     * @param message сообщение об ошибке
     * @param cause   причина возникновения исключения
     */
    public HotelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
