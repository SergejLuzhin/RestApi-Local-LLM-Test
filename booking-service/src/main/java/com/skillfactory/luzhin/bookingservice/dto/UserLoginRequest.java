package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO (Transfer Object) для запроса на авторизацию пользователя.
 *
 * Используется в эндпоинте POST /user/auth.
 *
 * Поля:
 * - username — имя пользователя для входа;
 * - password — пароль для проверки аутентификации.
 */
public class UserLoginRequest {

    /**
     * Имя пользователя для входа в систему.
     */
    @NotBlank(message = "Имя пользователя не может быть пустым")
    private String username;

    /**
     * Пароль для проверки аутентификации.
     */
    @NotBlank(message = "Пароль не может быть пустым")
    private String password;

    /**
     * Возвращает имя пользователя.
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Устанавливает имя пользователя.
     *
     * @param username имя пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Возвращает пароль.
     *
     * @return пароль
     */
    public String getPassword() {
        return password;
    }

    /**
     * Устанавливает пароль.
     *
     * @param password пароль
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
