package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO (Transfer Object) для запроса на обновление данных пользователя.
 *
 * Используется в эндпоинте PATCH /user.
 * Все поля опциональны — обновляются только переданные значения.
 *
 * Поля:
 * - username — новое имя пользователя (опционально, 3-50 символов);
 * - password — новый пароль (опционально, минимум 6 символов).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    /** Новое имя пользователя (опционально). От 3 до 50 символов, если передано. */
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
    private String username;

    /** Новый пароль (опционально). Минимум 6 символов, если передан. */
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;
}
