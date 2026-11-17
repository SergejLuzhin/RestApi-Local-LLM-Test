package com.skillfactory.luzhin.hotelservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Базовый класс сущностей, содержащий общие поля: время создания и время обновления.
 * Используется как родитель для всех JPA-сущностей в Hotel Service.
 */
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public abstract class BaseEntity {

    /** Время создания записи */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Время последнего обновления записи */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Метод-колбэк JPA, вызываемый перед сохранением новой записи.
     * Устанавливает время создания и обновления через Hibernate.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Метод-колбэк JPA, вызываемый перед обновлением записи.
     * Обновляет поле updatedAt через Hibernate.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
