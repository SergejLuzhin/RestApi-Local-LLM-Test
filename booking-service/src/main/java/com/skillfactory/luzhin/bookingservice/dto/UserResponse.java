package com.skillfactory.luzhin.bookingservice.dto;

import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для передачи информации о пользователе.
 *
 * Используется в ответах контроллеров для возврата данных о пользователе.
 * Пароль не включается в ответ из соображений безопасности.
 *
 * Поля:
 * - id — уникальный идентификатор пользователя;
 * - username — имя пользователя;
 * - role — роль пользователя (USER или ADMIN);
 * - createdAt — время создания записи;
 * - updatedAt — время последнего обновления.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /** Уникальный идентификатор пользователя. */
    private Long id;

    /** Имя пользователя для входа в систему. */
    private String username;

    /** Роль пользователя — определяет уровень доступа к функциям системы. */
    private UserRole role;

    /** Дата и время создания записи. */
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления записи. */
    private LocalDateTime updatedAt;
}
