package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO (Transfer Object) для запроса на регистрацию нового пользователя.
 *
 * Используется в эндпоинте POST /user/register.
 *
 * Поля:
 * - username — уникальное имя пользователя (обязательное, 3-50 символов);
 * - password — пароль для входа (обязательный, минимум 6 символов).
 */
public class UserRegisterRequest {

    /**
     * Уникальное имя пользователя для входа в систему.
     * Должно содержать от 3 до 50 символов.
     */
    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
    private String username;

    /**
     * Пароль для входа в систему.
     * Должен содержать минимум 6 символов.
     */
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
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
