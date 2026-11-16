package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для запроса на создание нового бронирования.
 *
 * Используется в эндпоинте POST /booking.
 *
 * Поля:
 * - roomId — идентификатор номера (обязательный, если autoSelect = false);
 * - startDate — дата и время начала бронирования (обязательная, будущая);
 * - endDate — дата и время окончания бронирования (обязательная, будущая, позже startDate);
 * - autoSelect — флаг автоподбора номера (true — номер подбирается автоматически на основе
 *                алгоритма равномерной загрузки, false — используется указанный roomId).
 */
public class BookingCreateRequest {

    /**
     * Идентификатор номера. Обязателен, если autoSelect = false.
     * Игнорируется, если autoSelect = true.
     */
    @NotNull(message = "Идентификатор номера не может быть null")
    private Long roomId;

    /**
     * Дата и время начала бронирования.
     * Должна быть в будущем.
     */
    @NotNull(message = "Дата начала не может быть null")
    @Future(message = "Дата начала должна быть в будущем")
    private LocalDateTime startDate;

    /**
     * Дата и время окончания бронирования.
     * Должна быть в будущем и позже даты начала.
     */
    @NotNull(message = "Дата окончания не может быть null")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDateTime endDate;

    /**
     * Флаг автоматического подбора номера.
     * true — номер подбирается автоматически на основе алгоритма
     *        равномерной загрузки (без учёта roomId);
     * false — используется указанный roomId.
     */
    private boolean autoSelect;

    /**
     * Возвращает идентификатор номера.
     *
     * @return идентификатор номера
     */
    public Long getRoomId() {
        return roomId;
    }

    /**
     * Устанавливает идентификатор номера.
     *
     * @param roomId идентификатор номера
     */
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    /**
     * Возвращает дату и время начала бронирования.
     *
     * @return дата начала
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Устанавливает дату и время начала бронирования.
     *
     * @param startDate дата начала
     */
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    /**
     * Возвращает дату и время окончания бронирования.
     *
     * @return дата окончания
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }

    /**
     * Устанавливает дату и время окончания бронирования.
     *
     * @param endDate дата окончания
     */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    /**
     * Возвращает флаг автоматического подбора номера.
     *
     * @return true — если автоподбор включён; false — в противном случае
     */
    public boolean isAutoSelect() {
        return autoSelect;
    }

    /**
     * Устанавливает флаг автоматического подбора номера.
     *
     * @param autoSelect true — включить автоподбор; false — указать roomId вручную
     */
    public void setAutoSelect(boolean autoSelect) {
        this.autoSelect = autoSelect;
    }
}
