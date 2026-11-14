package com.skillfactory.luzhin.hotelservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO для запроса создания нового отеля.
 * Содержит название и адрес отеля.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelCreateRequest {

    /**
     * Название отеля (обязательное поле, не более 255 символов).
     */
    @NotBlank(message = "Название отеля не может быть пустым")
    @Size(max = 255, message = "Название отеля не должно превышать 255 символов")
    private String name;

    /**
     * Адрес отеля (обязательное поле, не более 500 символов).
     */
    @NotBlank(message = "Адрес отеля не может быть пустым")
    @Size(max = 500, message = "Адрес отеля не должен превышать 500 символов")
    private String address;
}
