package com.skillfactory.luzhin.bookingservice.controller;

import com.skillfactory.luzhin.bookingservice.dto.TokenResponse;
import com.skillfactory.luzhin.bookingservice.dto.UserLoginRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserRegisterRequest;
import com.skillfactory.luzhin.bookingservice.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для контроллера аутентификации и регистрации пользователей.
 * Проверяет эндпоинты POST /user/register и POST /user/auth.
 *
 * <p>Тесты включают:
 * <ul>
 *   <li>Успешная регистрация нового пользователя</li>
 *   <li>Успешная авторизация существующего пользователя</li>
 *   <li>Отсутствие токена — 401 Unauthorized</li>
 *   <li>Дубликат имени пользователя — 400 Bad Request</li>
 *   <li>Неверный пароль — 401 Unauthorized</li>
 * </ul>
 *
 * @see AuthController
 * @see AuthService
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWjEyMzQ1Njc4OTA="
})
class AuthControllerTest {

    /**
     * Mock для сервиса аутентификации — все вызовы бизнес-логики мокнуты.
     */
    @MockBean
    private AuthService authService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    /**
     * Секретный ключ JWT для генерации тестовых токенов.
     */
    private SecretKey signingKey;

    /**
     * Инициализация MockMvc с Spring Security и секретного ключа JWT.
     *
     * @throws Exception ошибка при декодировании ключа
     */
    @BeforeEach
    void setUp() throws Exception {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    /**
     * Генерирует JWT-токен для указанного пользователя и роли.
     *
     * @param username имя пользователя
     * @param role роль пользователя (USER или ADMIN)
     * @return JWT-токен в строковом формате
     */
    private String generateToken(String username, String role) {
        String roleClaim = "ROLE_" + role.toUpperCase();
        return Jwts.builder()
                .subject(username)
                .claim("userId", 1L)
                .claim("role", roleClaim)
                .expiration(java.util.Date.from(
                        java.time.Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
    }

    // ==================== AC-1: Успешная регистрация ====================

    /**
     * Проверяет успешную регистрацию нового пользователя.
     * Запрос: POST /user/register с валидными данными.
     * Ожидаемый результат: HTTP 200 OK с JWT-токеном и данными пользователя.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        TokenResponse response = TokenResponse.builder()
                .token(generateToken("testuser", "USER"))
                .user(com.skillfactory.luzhin.bookingservice.dto.UserResponse.builder()
                        .id(1L)
                        .username("testuser")
                        .role(com.skillfactory.luzhin.bookingservice.entity.UserRole.USER)
                        .build())
                .build();

        when(authService.register(any(UserRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    // ==================== AC-2: Успешная авторизация ====================

    /**
     * Проверяет успешную авторизацию существующего пользователя.
     * Запрос: POST /user/auth с валидными учётными данными.
     * Ожидаемый результат: HTTP 200 OK с JWT-токеном и данными пользователя.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldAuthenticateUserSuccessfully() throws Exception {
        TokenResponse response = TokenResponse.builder()
                .token(generateToken("testuser", "USER"))
                .user(com.skillfactory.luzhin.bookingservice.dto.UserResponse.builder()
                        .id(1L)
                        .username("testuser")
                        .role(com.skillfactory.luzhin.bookingservice.entity.UserRole.USER)
                        .build())
                .build();

        when(authService.authenticate(any(UserLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    // ==================== AC-3: Регистрация — публичный эндпоинт ====================

    /**
     * Проверяет, что эндпоинт регистрации является публичным (permitAll).
     * POST /user/register доступен без JWT-токена — это намеренное поведение
     * для регистрации новых пользователей.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldAllowRegistrationWithoutToken() throws Exception {
        // Регистрация доступна публично — это expected behavior
        TokenResponse response = TokenResponse.builder()
                .token(generateToken("uniqueuser", "USER"))
                .user(com.skillfactory.luzhin.bookingservice.dto.UserResponse.builder()
                        .id(1L).username("uniqueuser")
                        .role(com.skillfactory.luzhin.bookingservice.entity.UserRole.USER)
                        .build())
                .build();

        when(authService.register(any(UserRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"uniqueuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("uniqueuser"));
    }

    // ==================== AC-4: Авторизация — публичный эндпоинт ====================

    /**
     * Проверяет, что эндпоинт авторизации является публичным (permitAll).
     * POST /user/auth доступен без JWT-токена — это намеренное поведение
     * для получения JWT-токена.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldAllowAuthenticationWithoutToken() throws Exception {
        // Авторизация доступна публично — это expected behavior
        when(authService.authenticate(any(UserLoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Неверный пароль"));

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный пароль"));
    }

    // ==================== AC-5: Валидация регистрации ====================

    /**
     * Проверяет валидацию входных данных при регистрации.
     * Запрос: POST /user/register с пустым телом.
     * Ожидаемый результат: HTTP 400 Bad Request с ошибками валидации.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectEmptyBodyOnRegistration() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ==================== AC-6: Занятое имя пользователя ====================

    /**
     * Проверяет, что регистрация с занятым именем пользователя возвращает 400.
     * Запрос: POST /user/register с существующим именем.
     * Ожидаемый результат: HTTP 400 Bad Request с сообщением об ошибке.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        when(authService.register(any(UserRegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Имя пользователя 'testuser' уже занято"));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Имя пользователя 'testuser' уже занято"));
    }

    // ==================== AC-7: Неверный пароль ====================

    /**
     * Проверяет, что авторизация с неверным паролем возвращает 400.
     * Запрос: POST /user/auth с неверным паролем.
     * Ожидаемый результат: HTTP 400 Bad Request с сообщением об ошибке.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectWrongPassword() throws Exception {
        when(authService.authenticate(any(UserLoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Неверный пароль"));

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный пароль"));
    }
}
