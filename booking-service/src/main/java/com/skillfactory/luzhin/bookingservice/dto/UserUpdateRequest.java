package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.Size;

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
public class UserUpdateRequest {

    /**
     * Новое имя пользователя (опциональное).
     * Должно содержать от 3 до 50 символов, если передано.
     */
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
    private String username;

    /**
     * Новый пароль (опциональный).
     * Должен содержать минимум 6 символов, если передан.
     */
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;

    /**
     * Возвращает новое имя пользователя.
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Устанавливает новое имя пользователя.
     *
     * @param username имя пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Возвращает новый пароль.
     *
     * @return пароль
     */
    public String getPassword() {
        return password;
    }

    /**
     * Устанавливает новый пароль.
     *
     * @param password пароль
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
