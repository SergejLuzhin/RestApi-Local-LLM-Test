package com.skillfactory.luzhin.bookingservice.dto;

import com.skillfactory.luzhin.bookingservice.entity.UserRole;

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
public class UserResponse {

    /**
     * Уникальный идентификатор пользователя.
     */
    private Long id;

    /**
     * Имя пользователя для входа в систему.
     */
    private String username;

    /**
     * Роль пользователя — определяет уровень доступа к функциям системы.
     */
    private UserRole role;

    /**
     * Дата и время создания записи.
     */
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления записи.
     */
    private LocalDateTime updatedAt;

    /**
     * Конструктор для создания ответа о пользователе.
     *
     * @param id      уникальный идентификатор
     * @param username имя пользователя
     * @param role    роль пользователя
     * @param createdAt время создания
     * @param updatedAt время последнего обновления
     */
    public UserResponse(Long id, String username, UserRole role,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Возвращает уникальный идентификатор пользователя.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает уникальный идентификатор пользователя.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

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
     * Возвращает роль пользователя.
     *
     * @return роль пользователя
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Устанавливает роль пользователя.
     *
     * @param role роль пользователя
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * Возвращает время создания записи.
     *
     * @return время создания
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Устанавливает время создания записи.
     *
     * @param createdAt время создания
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Возвращает время последнего обновления записи.
     *
     * @return время последнего обновления
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Устанавливает время последнего обновления записи.
     *
     * @param updatedAt время последнего обновления
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
