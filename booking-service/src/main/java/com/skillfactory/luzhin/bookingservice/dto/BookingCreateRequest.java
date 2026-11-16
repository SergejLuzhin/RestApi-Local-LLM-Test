package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для запроса на создание нового бронирования.
 *
 * Используется в эндпоинте POST /booking.
 *
 * Поля:
 * - roomId — идентификатор номера (обязательный, если autoSelect = false);
 * - startDate — дата и время начала бронирования (обязательная, будущая);
 * - endDate — дата и время окончания бронирования (обязательная, будущая, позже startDate);
 * - autoSelect — флаг автоподбора номера (true — номер подбирается автоматически
 *                на основе алгоритма равномерной загрузки, false — используется указанный roomId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    /** Идентификатор номера. Обязателен, если autoSelect = false. Игнорируется при autoSelect = true. */
    @NotNull(message = "Идентификатор номера не может быть null")
    private Long roomId;

    /** Дата и время начала бронирования. Должна быть в будущем. */
    @NotNull(message = "Дата начала не может быть null")
    @Future(message = "Дата начала должна быть в будущем")
    private LocalDateTime startDate;

    /** Дата и время окончания бронирования. Должна быть в будущем и позже даты начала. */
    @NotNull(message = "Дата окончания не может быть null")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDateTime endDate;

    /** Флаг автоматического подбора номера. true — автоподбор; false — использовать roomId. */
    @Builder.Default
    private boolean autoSelect = false;
}
