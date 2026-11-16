package com.skillfactory.luzhin.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность JPA «Пользователь системы».
 *
 * Представляет пользователя, который может взаимодействовать с системой бронирования.
 * Каждый пользователь имеет уникальное имя, хешированный пароль и роль доступа.
 * Пароли хранятся в зашифрованном виде (BCrypt).
 *
 * Поля:
 * - id — уникальный идентификатор (автогенерация);
 * - username — уникальное имя пользователя для входа;
 * - password — хешированный пароль (BCrypt);
 * - role — роль пользователя (USER или ADMIN);
 * - createdAt — время создания записи;
 * - updatedAt — время последнего обновления.
 */
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Уникальный идентификатор пользователя (автогенерация).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальное имя пользователя для входа в систему.
     * Уникальность обеспечивается индексом на уровне базы данных.
     */
    @Column(nullable = false, unique = true, length = 255)
    private String username;

    /**
     * Хешированный пароль (BCrypt).
     * Пароли хранятся только в зашифрованном виде.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Роль пользователя — определяет уровень доступа к функциям системы.
     * USER — личные операции (просмотр отелей, бронирование);
     * ADMIN — административные операции (управление отелями, номерами, пользователями).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    /**
     * Дата и время создания записи.
     * Заполняется автоматически при сохранении сущности.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления записи.
     * Заполняется автоматически при обновлении сущности.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Метод-колбэк, вызываемый перед сохранением новой записи.
     * Устанавливает дату и время создания.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Метод-колбэк, вызываемый перед обновлением записи.
     * Устанавливает дату и время последнего обновления.
     * Lombok @Getter генерирует только getter, setter отсутствует,
     * что предотвращает конфликты с JPA lifecycle callback.
     */
    @SuppressWarnings("squid:S1192")
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
