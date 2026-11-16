package com.skillfactory.luzhin.bookingservice.service;

import com.skillfactory.luzhin.bookingservice.config.JwtUtil;
import com.skillfactory.luzhin.bookingservice.dto.TokenResponse;
import com.skillfactory.luzhin.bookingservice.dto.UserLoginRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserRegisterRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserResponse;
import com.skillfactory.luzhin.bookingservice.entity.User;
import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.bookingservice.mapper.UserMapper;
import com.skillfactory.luzhin.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис аутентификации и авторизации пользователей.
 *
 * <p>Обеспечивает регистрацию новых пользователей, проверку учётных данных
 * и генерацию JWT-токенов для последующей аутентификации во всех сервисах системы.
 *
 * <p>Основная функциональность:
 * <ul>
 *   <li>{@code register} — создание нового пользователя с хешированием пароля (BCrypt)</li>
 *   <li>{@code authenticate} — проверка учётных данных и генерация JWT-токена</li>
 * </ul>
 *
 * <p>Токены генерируются со сроком действия 1 час (конфигурируется в {@code jwt.expiration-ms}).
 * Роль по умолчанию при регистрации — {@code USER}.
 *
 * @see JwtUtil
 * @see PasswordEncoder
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    /**
     * Репозиторий для операций с сущностью User.
     */
    private final UserRepository userRepository;

    /**
     * Маппер для преобразования между сущностью User и DTO.
     */
    private final UserMapper userMapper;

    /**
     * Кодировщик паролей BCrypt для хеширования и проверки паролей.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Утилита для генерации и валидации JWT-токенов.
     */
    private final JwtUtil jwtUtil;

    /**
     * Регистрирует нового пользователя в системе.
     *
     * <p>Процесс регистрации:
     * <ol>
     *   <li>Проверяет, не занято ли имя пользователя</li>
     *   <li>Создаёт сущность User из запроса регистрации</li>
     *   <li>Хеширует пароль с помощью BCrypt</li>
     *   <li>Устанавливает роль USER по умолчанию</li>
     *   <li>Сохраняет пользователя в базе данных</li>
     *   <li>Генерирует JWT-токен для нового пользователя</li>
     * </ol>
     *
     * @param request запрос на регистрацию, содержащий имя пользователя и пароль
     * @return ответ с JWT-токеном и данными нового пользователя
     * @throws IllegalArgumentException если имя пользователя уже занято
     */
    @Transactional
    public TokenResponse register(UserRegisterRequest request) {
        // Проверяем, не занято ли имя пользователя
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя '" + request.getUsername() + "' уже занято");
        }

        // Создаём сущность User из запроса (username копируется через маппер)
        User user = userMapper.toEntity(request);

        // Хешируем пароль и устанавливаем роль USER по умолчанию
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);

        // Сохраняем пользователя в базе данных
        User savedUser = userRepository.save(user);

        // Генерируем JWT-токен для нового пользователя
        String token = jwtUtil.generateToken(savedUser.getUsername(), "ROLE_USER");

        // Формируем ответ с токеном
        UserResponse response = userMapper.toResponse(savedUser);
        return TokenResponse.builder()
                .token(token)
                .user(response)
                .build();
    }

    /**
     * Авторизует пользователя по имени и паролю.
     *
     * <p>Процесс авторизации:
     * <ol>
     *   <li>Находит пользователя по имени</li>
     *   <li>Проверяет пароль через BCrypt</li>
     *   <li>Генерирует JWT-токен с ролью пользователя</li>
     * </ol>
     *
     * @param request запрос на авторизацию, содержащий имя пользователя и пароль
     * @return ответ с JWT-токеном и данными пользователя
     * @throws ResourceNotFoundException если пользователь с таким именем не найден
     * @throws IllegalArgumentException если пароль неверный
     */
    public TokenResponse authenticate(UserLoginRequest request) {
        // Ищем пользователя по имени
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", request.getUsername()));

        // Проверяем пароль
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Неверный пароль");
        }

        // Генерируем JWT-токен с ролью пользователя
        String role = "ROLE_" + user.getRole().name();
        String token = jwtUtil.generateToken(user.getUsername(), role);

        // Формируем ответ с токеном
        UserResponse response = userMapper.toResponse(user);
        return TokenResponse.builder()
                .token(token)
                .user(response)
                .build();
    }
}
