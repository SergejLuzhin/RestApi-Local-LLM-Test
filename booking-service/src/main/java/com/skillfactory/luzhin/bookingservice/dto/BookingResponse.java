package com.skillfactory.luzhin.bookingservice.dto;

import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import lombok.*;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    /** Уникальный идентификатор бронирования. */
    private Long id;

    /** Идентификатор пользователя, создавшего бронирование. */
    private Long userId;

    /** Имя пользователя, создавшего бронирование. */
    private String username;

    /** Идентификатор номера в отеле. */
    private Long roomId;

    /** Дата и время начала бронирования. */
    private LocalDateTime startDate;

    /** Дата и время окончания бронирования. */
    private LocalDateTime endDate;

    /** Текущий статус бронирования. PENDING, CONFIRMED или CANCELLED. */
    private BookingStatus status;

    /** Дата и время создания записи. */
    private LocalDateTime createdAt;
}
