package com.skillfactory.luzhin.bookingservice.repository;

import com.skillfactory.luzhin.bookingservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для выполнения CRUD-операций с сущностью {@link User}.
 *
 * Предоставляет методы для поиска пользователей по имени пользователя,
 * а также проверки уникальности имён.
 *
 * Методы:
 * - findByUsername — поиск пользователя по уникальному имени;
 * - existsByUsername — проверка существования пользователя с указанным именем.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по уникальному имени пользователя.
     *
     * @param username имя пользователя для поиска
     * @return Optional с найденным пользователем, пустой — если пользователь не найден
     */
    Optional<User> findByUsername(String username);

    /**
     * Проверяет, существует ли пользователь с указанным именем.
     *
     * @param username имя пользователя для проверки
     * @return true, если пользователь с таким именем уже существует; false — в противном случае
     */
    boolean existsByUsername(String username);
}
