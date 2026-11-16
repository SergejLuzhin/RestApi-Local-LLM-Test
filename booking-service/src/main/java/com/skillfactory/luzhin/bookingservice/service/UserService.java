package com.skillfactory.luzhin.bookingservice.service;

import com.skillfactory.luzhin.bookingservice.dto.UserRegisterRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserResponse;
import com.skillfactory.luzhin.bookingservice.dto.UserUpdateRequest;
import com.skillfactory.luzhin.bookingservice.entity.User;
import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.bookingservice.mapper.UserMapper;
import com.skillfactory.luzhin.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис управления пользователями (административный).
 *
 * <p>Обеспечивает CRUD-операции с пользователями для администраторов системы.
 * Все операции этого сервиса доступны только пользователям с ролью ADMIN.
 *
 * <p>Основная функциональность:
 * <ul>
 *   <li>{@code createUser} — создание нового пользователя с указанной ролью</li>
 *   <li>{@code getAllUsers} — получение списка всех пользователей</li>
 *   <li>{@code getUserById} — получение пользователя по идентификатору</li>
 *   <li>{@code updateUser} — обновление данных пользователя</li>
 *   <li>{@code deleteUser} — удаление пользователя из системы</li>
 * </ul>
 *
 * <p>Пароли хранятся в зашифрованном виде (BCrypt).
 * При создании нового пользователя роль задаётся явно через запрос.
 * При обновлении пароля он автоматически хешируется.
 *
 * @see PasswordEncoder
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    /**
     * Репозиторий для операций с сущностью User.
     */
    private final UserRepository userRepository;

    /**
     * Маппер для преобразования между сущностью User и DTO.
     */
    private final UserMapper userMapper;

    /**
     * Кодировщик паролей BCrypt для хеширования паролей.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Создаёт нового пользователя с указанной ролью.
     *
     * <p>Процесс создания:
     * <ol>
     *   <li>Проверяет, не занято ли имя пользователя</li>
     *   <li>Создаёт сущность User из запроса регистрации</li>
     *   <li>Хеширует пароль через BCrypt</li>
     *   <li>Устанавливает роль из запроса (или USER по умолчанию)</li>
     *   <li>Сохраняет пользователя в базе данных</li>
     * </ol>
     *
     * @param request запрос на создание пользователя
     * @return DTO с данными созданного пользователя
     * @throws IllegalArgumentException если имя пользователя уже занято
     */
    @Transactional
    public UserResponse createUser(UserRegisterRequest request) {
        // Проверяем, не занято ли имя пользователя
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя '" + request.getUsername() + "' уже занято");
        }

        // Создаём сущность User из запроса
        User user = userMapper.toEntity(request);

        // Хешируем пароль и устанавливаем роль USER по умолчанию
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);

        // Сохраняем и возвращаем
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    /**
     * Создаёт нового пользователя с указанной ролью ADMIN.
     *
     * <p>В отличие от {@link #createUser(UserRegisterRequest)},
     * этот метод позволяет явно задать роль пользователя через {@code UserUpdateRequest}.
     * Используется администраторами для создания пользователей с определённой ролью.
     *
     * @param username имя пользователя
     * @param password пароль пользователя
     * @param role роль пользователя (USER или ADMIN)
     * @return DTO с данными созданного пользователя
     * @throws IllegalArgumentException если имя пользователя уже занято
     */
    @Transactional
    public UserResponse createUser(String username, String password, UserRole role) {
        // Проверяем, не занято ли имя пользователя
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Имя пользователя '" + username + "' уже занято");
        }

        // Создаём нового пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        // Сохраняем и возвращаем
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    /**
     * Получает список всех пользователей системы.
     *
     * @return список DTO с данными всех пользователей
     */
    public List<UserResponse> getAllUsers() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    /**
     * Получает пользователя по уникальному идентификатору.
     *
     * @param id идентификатор пользователя
     * @return DTO с данными пользователя
     * @throws ResourceNotFoundException если пользователь с таким идентификатором не найден
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
        return userMapper.toResponse(user);
    }

    /**
     * Обновляет данные существующего пользователя.
     *
     * <p>Процесс обновления:
     * <ol>
     *   <li>Находит пользователя по идентификатору</li>
     *   <li>Обновляет имя пользователя (если передано)</li>
     *   <li>Обновляет пароль (если передано) — с хешированием</li>
     *   <li>Сохраняет обновлённого пользователя</li>
     * </ol>
     *
     * @param id        идентификатор пользователя для обновления
     * @param request   запрос с обновлёнными данными
     * @return DTO с обновлёнными данными пользователя
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        // Обновляем имя пользователя (если передано)
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }

        // Обновляем пароль (если передан) — с хешированием
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    /**
     * Удаляет пользователя по уникальному идентификатору.
     *
     * <p>Если у пользователя есть бронирования, они будут удалены каскадно
     * (настройка CascadeType.ALL на связи OneToMany в сущности User).
     *
     * @param id идентификатор пользователя для удаления
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь", id);
        }
        userRepository.deleteById(id);
    }
}
