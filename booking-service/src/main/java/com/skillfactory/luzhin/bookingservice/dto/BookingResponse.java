package com.skillfactory.luzhin.bookingservice.dto;

import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для передачи информации о бронировании.
 *
 * Используется в ответах контроллеров для возврата данных о бронировании.
 * Содержит полную информацию о бронировании, включая данные о пользователе.
 *
 * Поля:
 * - id — уникальный идентификатор бронирования;
 * - userId — идентификатор пользователя, создавшего бронирование;
 * - username — имя пользователя, создавшего бронирование;
 * - roomId — идентификатор номера;
 * - startDate — дата и время начала бронирования;
 * - endDate — дата и время окончания бронирования;
 * - status — текущий статус бронирования;
 * - createdAt — время создания записи.
 */
public class BookingResponse {

    /**
     * Уникальный идентификатор бронирования.
     */
    private Long id;

    /**
     * Идентификатор пользователя, создавшего бронирование.
     */
    private Long userId;

    /**
     * Имя пользователя, создавшего бронирование.
     */
    private String username;

    /**
     * Идентификатор номера в отеле.
     */
    private Long roomId;

    /**
     * Дата и время начала бронирования.
     */
    private LocalDateTime startDate;

    /**
     * Дата и время окончания бронирования.
     */
    private LocalDateTime endDate;

    /**
     * Текущий статус бронирования.
     * PENDING, CONFIRMED или CANCELLED.
     */
    private BookingStatus status;

    /**
     * Дата и время создания записи.
     */
    private LocalDateTime createdAt;

    /**
     * Конструктор для создания ответа о бронировании.
     *
     * @param id       уникальный идентификатор бронирования
     * @param userId   идентификатор пользователя
     * @param username имя пользователя
     * @param roomId   идентификатор номера
     * @param startDate дата начала бронирования
     * @param endDate   дата окончания бронирования
     * @param status   текущий статус бронирования
     * @param createdAt время создания записи
     */
    public BookingResponse(Long id, Long userId, String username, Long roomId,
                           LocalDateTime startDate, LocalDateTime endDate,
                           BookingStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Возвращает уникальный идентификатор бронирования.
     *
     * @return идентификатор бронирования
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает уникальный идентификатор бронирования.
     *
     * @param id идентификатор бронирования
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор пользователя.
     *
     * @return идентификатор пользователя
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Устанавливает идентификатор пользователя.
     *
     * @param userId идентификатор пользователя
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Возвращает имя пользователя.
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Устанавливает имя пользователя.
     *
     * @param username имя пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

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
     * Возвращает текущий статус бронирования.
     *
     * @return статус бронирования
     */
    public BookingStatus getStatus() {
        return status;
    }

    /**
     * Устанавливает текущий статус бронирования.
     *
     * @param status статус бронирования
     */
    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    /**
     * Возвращает время создания записи.
     *
     * @return время создания
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Устанавливает время создания записи.
     *
     * @param createdAt время создания
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
