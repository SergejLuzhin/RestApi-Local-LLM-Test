package com.skillfactory.luzhin.hotelservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа с информацией об отеле.
 * Содержит идентификатор, название, адрес, количество номеров, время создания и обновления.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelResponse {

    /**
     * Уникальный идентификатор отеля.
     */
    private Long id;

    /**
     * Название отеля.
     */
    private String name;

    /**
     * Адрес отеля.
     */
    private String address;

    /**
     * Общее количество номеров в отеле.
     */
    private Integer totalRooms;

    /**
     * Количество номеров, которые операционно доступны.
     */
    private Integer availableRooms;

    /**
     * Время создания записи.
     */
    private LocalDateTime createdAt;

    /**
     * Время последнего обновления записи.
     */
    private LocalDateTime updatedAt;
}
