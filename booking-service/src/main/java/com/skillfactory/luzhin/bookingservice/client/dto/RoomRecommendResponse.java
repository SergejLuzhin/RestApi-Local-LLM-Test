package com.skillfactory.luzhin.bookingservice.client.dto;

import lombok.*;

/**
 * DTO (Transfer Object) для ответа от Hotel Service при запросе рекомендованных номеров.
 *
 * <p>Используется Feign-клиентом для десериализации HTTP-ответа
 * от эндпоинта {@code GET /api/rooms/recommend}.
 *
 * <p>Содержит информацию о свободных номерах, отсортированных
 * по возрастанию счётчика бронирований (для алгоритма равномерной загрузки).
 *
 * <p>Поля:
 * <ul>
 *   <li>{@code id} — уникальный идентификатор номера;</li>
 *   <li>{@code hotelId} — идентификатор отеля, к которому относится номер;</li>
 *   <li>{@code hotelName} — название отеля;</li>
 *   <li>{@code number} — обозначение номера;</li>
 *   <li>{@code available} — флаг операционной доступности;</li>
 *   <li>{@code timesBooked} — количество бронирований (для сортировки);</li>
 *   <li>{@code createdAt} — дата и время создания записи;</li>
 *   <li>{@code updatedAt} — дата и время последнего обновления записи.</li>
 * </ul>
 *
 * @see com.skillfactory.luzhin.hotelservice.controller.RoomController#getRecommendedRooms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRecommendResponse {

    /** Уникальный идентификатор номера. */
    private Long id;

    /** Идентификатор отеля, к которому принадлежит номер. */
    private Long hotelId;

    /** Название отеля. */
    private String hotelName;

    /** Обозначение номера в отеле. */
    private String number;

    /** Флаг операционной доступности номера. true — номер доступен для бронирования. */
    private boolean available;

    /** Количество бронирований номера. Используется для алгоритма равномерной загрузки. */
    private int timesBooked;

    /** Дата и время создания записи о номере. */
    private java.time.LocalDateTime createdAt;

    /** Дата и время последнего обновления записи о номере. */
    private java.time.LocalDateTime updatedAt;
}
