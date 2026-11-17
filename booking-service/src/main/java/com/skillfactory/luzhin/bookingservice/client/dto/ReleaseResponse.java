package com.skillfactory.luzhin.bookingservice.client.dto;

import lombok.*;

/**
 * DTO (Transfer Object) для ответа от Hotel Service при снятии временной блокировки.
 *
 * <p>Используется Feign-клиентом для десериализации HTTP-ответа
 * от внутреннего эндпоинта <code>POST /rooms/{roomId}/release</code>.
 *
 * <p>Содержит информацию о результате компенсации (снятия блокировки слота).
 *
 * @see com.skillfactory.luzhin.hotelservice.controller.RoomController
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseResponse {

    /** Идентификатор номера, для которого была снята блокировка. */
    private Long roomId;

    /** Флаг, указывающий, что блокировка успешно снята. */
    private boolean released;

    /** Сообщение с результатом операции снятия блокировки. */
    private String message;
}
