package com.skillfactory.luzhin.hotelservice.exception;

/**
 * Исключение, выбрасываемое при отсутствии доступности номера на запрошенные даты.
 * Используется в методе confirmAvailability при обнаружении пересечения бронирований.
 */
public class RoomNotAvailableException extends RuntimeException {

    /**
     * Создаёт новое исключение с указанием идентификатора номера и сообщения об ошибке.
     *
     * @param roomId идентификатор номера, который недоступен
     * @param message сообщение об ошибке
     */
    public RoomNotAvailableException(Long roomId, String message) {
        super("Room " + roomId + " is not available: " + message);
    }

    /**
     * Создаёт новое исключение с указанием идентификатора номера и исходной причины.
     *
     * @param roomId идентификатор номера, который недоступен
     * @param cause исходная причина исключения
     */
    public RoomNotAvailableException(Long roomId, Throwable cause) {
        super("Room " + roomId + " is not available", cause);
    }
}
