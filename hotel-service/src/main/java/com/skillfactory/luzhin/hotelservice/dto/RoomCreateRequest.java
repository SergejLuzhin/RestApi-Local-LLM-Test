package com.skillfactory.luzhin.hotelservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO для запроса создания нового номера в отеле.
 * Содержит идентификатор отеля, номер комнаты и информацию о доступности.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateRequest {

    /**
     * Идентификатор отеля, в который добавляется номер (обязательное поле).
     */
    @NotNull(message = "Идентификатор отеля не может быть null")
    @Min(value = 1, message = "Идентификатор отеля должен быть больше нуля")
    private Long hotelId;

    /**
     * Номер комнаты (обозначение). Обязательное поле.
     */
    @NotBlank(message = "Номер комнаты не может быть пустым")
    @Size(max = 50, message = "Номер комнаты не должен превышать 50 символов")
    private String number;

    /**
     * Операционная доступность номера (по умолчанию true).
     * Если false — номер выведен из эксплуатации или на ремонте.
     */
    @Builder.Default
    private Boolean available = true;
}
