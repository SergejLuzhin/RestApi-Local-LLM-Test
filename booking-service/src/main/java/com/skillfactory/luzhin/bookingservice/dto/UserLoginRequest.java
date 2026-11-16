package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO (Transfer Object) для запроса на авторизацию пользователя.
 *
 * Используется в эндпоинте POST /user/auth.
 *
 * Поля:
 * - username — имя пользователя для входа;
 * - password — пароль для проверки аутентификации.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginRequest {

    /** Имя пользователя для входа в систему. */
    @NotBlank(message = "Имя пользователя не может быть пустым")
    private String username;

    /** Пароль для проверки аутентификации. */
    @NotBlank(message = "Пароль не может быть пустым")
    private String password;
}
