package com.skillfactory.luzhin.bookingservice.dto;

import lombok.*;

/**
 * DTO (Transfer Object) для ответа с JWT-токеном и данными пользователя.
 *
 * Используется в эндпоинтах регистрации и авторизации для возврата
 * JWT-токена вместе с данными авторизованного пользователя.
 *
 * Поля:
 * - token — сгенерированный JWT-токен для последующей аутентификации;
 * - user — данные пользователя (без пароля).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {

    /** JWT-токен для аутентификации в системе. Срок действия — 1 час. */
    private String token;

    /** Данные авторизованного пользователя (без пароля). */
    private UserResponse user;
}
