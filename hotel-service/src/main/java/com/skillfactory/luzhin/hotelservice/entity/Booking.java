package com.skillfactory.luzhin.hotelservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Локальная сущность бронирования в Hotel Service.
 * Используется исключительно для проверки пересечений дат бронирований
 * при подтверждении доступности номера (confirm-availability).
 * Содержит минимальный набор полей, необходимых для проверки доступности.
 * Полная сущность Booking находится в Booking Service.
 */
@Entity
@Table(name = "bookings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Booking extends BaseEntity {

    /**
     * Уникальный идентификатор бронирования.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Идентификатор пользователя (не используется как foreign key,
     * так как данные хранятся в Booking Service).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Идентификатор номера. Используется для проверки пересечений дат.
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
     * Статус бронирования: PENDING, CONFIRMED, CANCELLED.
     * Для проверки доступности учитываются только PENDING и CONFIRMED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    /**
     * Статус бронирования.
     */
    public enum BookingStatus {
        /** Бронирование создано, ожидает подтверждения доступности */
        PENDING,
        /** Бронирование подтверждено */
        CONFIRMED,
        /** Бронирование отменено */
        CANCELLED
    }
}
