package com.skillfactory.luzhin.bookingservice.exception;

/**
 * Исключение для кода HTTP 404 Not Found.
 * Выбрасывается, когда запрашиваемый ресурс не найден в базе данных.
 *
 * <p>Примеры использования:
 * <ul>
 *   <li>Пользователь с указанным ID не найден</li>
 *   <li>Бронирование с указанным ID не найдено</li>
 *   <li>Отель/номер с указанным ID не найден</li>
 * </ul>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Создаёт исключение с описанием не найденного ресурса и его ID.
     *
     * @param resourceName имя ресурса (например, "User", "Booking")
     * @param id           идентификатор не найденного ресурса
     */
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id));
    }
}
