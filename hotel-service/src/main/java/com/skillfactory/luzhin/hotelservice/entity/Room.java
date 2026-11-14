package com.skillfactory.luzhin.hotelservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность «Номер». Представляет номер в отеле с информацией о доступности
 * и количестве бронирований для алгоритма равномерной загрузки.
 */
@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Room extends BaseEntity {

    /**
     * Уникальный идентификатор номера.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ссылка на отель, которому принадлежит номер.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    /**
     * Номер комнаты (обозначение).
     */
    @Column(name = "number", nullable = false, length = 50)
    private String number;

    /**
     * Операционная доступность номера (true — доступен, false — выведен из эксплуатации/ремонт).
     * Не используется для проверки занятости по датам; занятость определяется через таблицу бронирований.
     */
    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    /**
     * Общее количество бронирований номера (используется для сортировки при автоподборе).
     * Стремясь к равномерной загрузке, Booking Service предлагает номера с меньшим значением timesBooked.
     */
    @Column(name = "times_booked", nullable = false)
    @Builder.Default
    private Integer timesBooked = 0;
}
