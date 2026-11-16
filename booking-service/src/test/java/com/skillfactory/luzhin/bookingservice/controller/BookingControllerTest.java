package com.skillfactory.luzhin.bookingservice.controller;

import com.skillfactory.luzhin.bookingservice.config.JwtUtil;
import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import com.skillfactory.luzhin.bookingservice.service.AuthService;
import com.skillfactory.luzhin.bookingservice.service.BookingService;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для контроллера управления бронированиями.
 * Проверяет эндпоинты POST /booking, GET /booking/{id}, GET /booking/bookings, DELETE /booking/{id}.
 *
 * <p>Тесты включают:
 * <ul>
 *   <li>Создание бронирования с явным roomId</li>
 *   <li>Создание бронирования с автоподбором номера</li>
 *   <li>Получение бронирования по ID</li>
 *   <li>Получение истории бронирований</li>
 *   <li>Отмена бронирования</li>
 *   <li>Бронирование не найдено — 404</li>
 *   <li>Доступ без токена — 401</li>
 * </ul>
 *
 * @see BookingController
 * @see BookingService
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWjEyMzQ1Njc4OTA="
})
class BookingControllerTest {

    /**
     * Mock для сервиса бронирований — все вызовы бизнес-логики мокнуты.
     */
    @MockBean
    private BookingService bookingService;

    /**
     * Mock для сервиса аутентификации — используется для проверки JWT.
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
     * @param userId идентификатор пользователя
     * @param username имя пользователя
     * @param role роль пользователя (USER или ADMIN)
     * @return JWT-токен в строковом формате
     */
    private String generateToken(Long userId, String username, String role) {
        String roleClaim = "ROLE_" + role.toUpperCase();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", roleClaim)
                .expiration(java.util.Date.from(
                        java.time.Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Создаёт DTO ответа для тестового бронирования.
     */
    private BookingResponse createBookingResponse(Long id, Long roomId, BookingStatus status) {
        return BookingResponse.builder()
                .id(id)
                .userId(1L)
                .username("testuser")
                .roomId(roomId)
                .startDate(LocalDateTime.of(2026, 12, 1, 14, 0))
                .endDate(LocalDateTime.of(2026, 12, 5, 11, 0))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== BC-1: Создание бронирования с явным roomId ====================

    /**
     * Проверяет успешное создание бронирования с явным указанием номера.
     * Запрос: POST /booking с autoSelect=false и roomId=5.
     * Ожидаемый результат: HTTP 201 Created с DTO бронирования.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldCreateBookingWithExplicitRoomId() throws Exception {
        BookingResponse response = createBookingResponse(1L, 5L, BookingStatus.CONFIRMED);

        when(bookingService.createBooking(org.mockito.ArgumentMatchers.any(BookingCreateRequest.class), anyLong()))
                .thenReturn(response);

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":5,\"startDate\":\"2026-12-01T14:00:00\","
                                + "\"endDate\":\"2026-12-05T11:00:00\",\"autoSelect\":false}")
                        .header("Authorization", "Bearer " + generateToken(1L, "testuser", "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomId").value(5))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== BC-2: Создание бронирования с автоподбором ====================

    /**
     * Проверяет успешное создание бронирования с автоподбором номера.
     * Запрос: POST /booking с autoSelect=true.
     * Ожидаемый результат: HTTP 201 Created с DTO бронирования.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldCreateBookingWithAutoSelect() throws Exception {
        BookingResponse response = createBookingResponse(1L, 10L, BookingStatus.CONFIRMED);

        when(bookingService.createBooking(org.mockito.ArgumentMatchers.any(BookingCreateRequest.class), anyLong()))
                .thenReturn(response);

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":10,\"startDate\":\"2026-12-01T14:00:00\","
                                + "\"endDate\":\"2026-12-05T11:00:00\",\"autoSelect\":true}")
                        .header("Authorization", "Bearer " + generateToken(1L, "testuser", "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomId").value(10))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== BC-3: Получение бронирования по ID ====================

    /**
     * Проверяет успешное получение бронирования по уникальному идентификатору.
     * Запрос: GET /booking/1.
     * Ожидаемый результат: HTTP 200 OK с DTO бронирования.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldGetBookingById() throws Exception {
        BookingResponse response = createBookingResponse(1L, 5L, BookingStatus.CONFIRMED);

        when(bookingService.getBookingById(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/booking/1")
                        .header("Authorization", "Bearer " + generateToken(1L, "testuser", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== BC-4: Получение истории бронирований ====================

    /**
     * Проверяет успешное получение истории бронирований текущего пользователя.
     * Запрос: GET /booking/bookings.
     * Ожидаемый результат: HTTP 200 OK со списком бронирований.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldGetBookingHistory() throws Exception {
        List<BookingResponse> bookings = List.of(
                createBookingResponse(1L, 5L, BookingStatus.CONFIRMED),
                createBookingResponse(2L, 7L, BookingStatus.CANCELLED)
        );

        when(bookingService.getBookingsByUserId(anyLong())).thenReturn(bookings);

        mockMvc.perform(get("/booking/bookings")
                        .header("Authorization", "Bearer " + generateToken(1L, "testuser", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    // ==================== BC-5: Отмена бронирования ====================

    /**
     * Проверяет успешную отмену бронирования.
     * Запрос: DELETE /booking/1.
     * Ожидаемый результат: HTTP 204 No Content.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldCancelBooking() throws Exception {
        mockMvc.perform(delete("/booking/1")
                        .header("Authorization", "Bearer " + generateToken(1L, "testuser", "USER")))
                .andExpect(status().isNoContent());
    }

    // ==================== BC-6: Бронирование не найдено ====================

    /**
     * Проверяет, что запрос несуществующего бронирования возвращает 404.
     * Запрос: GET /booking/999.
     * Ожидаемый результат: HTTP 404 Not Found с сообщением об ошибке.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldReturnNotFoundForMissingBooking() throws Exception {
        when(bookingService.getBookingById(anyLong(), anyLong()))
                .thenThrow(new com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException(
                        "Бронирование", 999L));

        mockMvc.perform(get("/booking/999")
                        .header("Authorization", "Bearer " + generateToken(1L, "testuser", "USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ==================== BC-7: Доступ без токена ====================

    /**
     * Проверяет, что доступ к бронированиям без JWT-токена возвращает 401.
     * Запрос: GET /booking/1 без заголовка Authorization.
     * Ожидаемый результат: HTTP 401 Unauthorized.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/booking/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== BC-8: Создание бронирования без токена ====================

    /**
     * Проверяет, что создание бронирования без JWT-токена возвращает 401.
     * Запрос: POST /booking без заголовка Authorization.
     * Ожидаемый результат: HTTP 401 Unauthorized.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectBookingCreationWithoutToken() throws Exception {
        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":5,\"startDate\":\"2026-12-01T14:00:00\","
                                + "\"endDate\":\"2026-12-05T11:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }
}
