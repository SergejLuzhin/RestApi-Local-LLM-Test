package com.skillfactory.luzhin.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность JPA «Бронирование».
 *
 * Представляет бронирование номера в отеле, связанное с конкретным пользователем.
 * Бронирование имеет статус, определяющий текущую стадию процесса:
 * PENDING — ожидает подтверждения от Hotel Service;
 * CONFIRMED — подтверждено и активно;
 * CANCELLED — отменено.
 *
 * Поля:
 * - id — уникальный идентификатор бронирования (автогенерация);
 * - user — пользователь, создавший бронирование;
 * - roomId — идентификатор номера (без внешнего ключа, так как номер хранится в Hotel Service);
 * - startDate — дата и время начала бронирования;
 * - endDate — дата и время окончания бронирования;
 * - status — текущий статус бронирования;
 * - createdAt — время создания записи;
 * - correlationId — идентификатор корреляции для сквозного отслеживания процесса бронирования.
 */
@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "idx_booking_user_id", columnList = "user_id"),
           @Index(name = "idx_booking_correlation_id", columnList = "correlation_id"),
           @Index(name = "idx_booking_room_id_start_end", columnList = "room_id, start_date, end_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    /**
     * Уникальный идентификатор бронирования (автогенерация).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь, создавший бронирование.
     * Связь ManyToOne с сущностью User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Идентификатор номера в отеле.
     * Не является внешним ключом, так как номер хранится в Hotel Service (микросервисная архитектура).
     */
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /**
     * Дата и время начала бронирования.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * Дата и время окончания бронирования.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /**
     * Текущий статус бронирования.
     * Определяет стадию жизненного цикла: PENDING, CONFIRMED или CANCELLED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingStatus status;

    /**
     * Дата и время создания записи.
     * Заполняется автоматически при сохранении сущности.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Идентикатор корреляции для сквозного отслеживания процесса бронирования.
     * Используется для обеспечения идемпотентности и логирования всех шагов процесса.
     * Формируется как UUID при создании бронирования.
     */
    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    /**
     * Метод-колбэк, вызываемый перед сохранением новой записи.
     * Устанавливает дату и время создания.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Проверяет, пересекаются ли даты данного бронирования с указанным периодом.
     *
     * @param start дата начала проверяемого периода
     * @param end   дата окончания проверяемого периода
     * @return true, если даты пересекаются; false в противном случае
     */
    public boolean hasDateOverlap(LocalDateTime start, LocalDateTime end) {
        return this.startDate.isBefore(end) && this.endDate.isAfter(start);
    }
}
