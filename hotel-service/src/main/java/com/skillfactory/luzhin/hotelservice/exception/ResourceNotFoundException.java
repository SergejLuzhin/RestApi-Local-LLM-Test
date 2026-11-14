package com.skillfactory.luzhin.hotelservice.exception;

/**
 * Исключение, выбрасываемое при отсутствии запрашиваемого ресурса.
 * Используется, когда отель, номер или бронирование не найдены по идентификатору.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Создаёт новое исключение с указанием типа ресурса и его идентификатора.
     *
     * @param resourceName название ресурса (например, «Hotel», «Room»)
     * @param id идентификатор не найденного ресурса
     */
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
