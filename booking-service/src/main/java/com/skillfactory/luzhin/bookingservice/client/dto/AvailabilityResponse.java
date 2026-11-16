package com.skillfactory.luzhin.bookingservice.client.dto;

import lombok.*;

/**
 * DTO (Transfer Object) для ответа от Hotel Service при подтверждении доступности номера.
 *
 * <p>Используется Feign-клиентом для десериализации HTTP-ответа
 * от внутреннего эндпоинта <code>POST /api/rooms/{roomId}/confirm-availability</code>.
 *
 * <p>Содержит информацию о результате проверки доступности номера на указанные даты.
 *
 * @see com.skillfactory.luzhin.hotelservice.controller.RoomController#confirmAvailability
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {

    /** Идентификатор номера, для которого была проверена доступность. */
    private Long roomId;

    /** Флаг, указывающий, что номер доступен на запрошенный период. */
    private boolean available;

    /** Сообщение с результатом проверки доступности. */
    private String message;
}
