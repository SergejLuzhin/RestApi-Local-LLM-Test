package com.skillfactory.luzhin.hotelservice.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией о номере.
 * Содержит идентификатор, информацию об отеле, номер комнаты, доступность и статистику бронирований.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    /**
     * Уникальный идентификатор номера.
     */
    private Long id;

    /**
     * Идентификатор отеля, которому принадлежит номер.
     */
    private Long hotelId;

    /**
     * Название отеля, которому принадлежит номер.
     */
    private String hotelName;

    /**
     * Номер комнаты (обозначение).
     */
    private String number;

    /**
     * Операционная доступность номера (true — доступен, false — выведен из эксплуатации/ремонт).
     */
    private Boolean available;

    /**
     * Общее количество бронирований номера. Используется для алгоритма равномерной загрузки.
     */
    private Integer timesBooked;

    /**
     * Время создания записи.
     */
    private LocalDateTime createdAt;

    /**
     * Время последнего обновления записи.
     */
    private LocalDateTime updatedAt;
}
