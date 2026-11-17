package com.skillfactory.luzhin.bookingservice.controller;

import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
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
 * Проверяет эндпоинты POST /bookings, GET /bookings/{id}, GET /bookings/bookings, DELETE /bookings/{id}.
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
     * Примечание: @MockBean deprecated since Spring Boot 3.4, @MockitoBean replacement
     * not yet stable — ошибка IDE игнорируется, требуется настройка IDE inspections.
     */
    @MockBean
    private BookingService bookingService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Spring @Value injects at runtime, IDE doesn't recognize
    @Value("${jwt.secret}")
    private String jwtSecret;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Spring @Autowired injects at runtime, IDE doesn't recognize
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    /**
     * Секретный ключ JWT для генерации тестовых токенов.
     */
    private SecretKey signingKey;

    /**
     * Тестовый JWT-токен для авторизации запросов.
     */
    private String authToken;

    /**
     * Инициализация MockMvc с Spring Security и секретного ключа JWT.
     */
    @BeforeEach
    void setUp() {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        this.authToken = generateToken();
    }

    /**
     * Генерирует тестовый JWT-токен для пользователя testuser с ролью USER.
     *
     * @return JWT-токен в строковом формате
     */
    private String generateToken() {
        return Jwts.builder()
                .subject("testuser")
                .claim("userId", 1L)
                .claim("role", "ROLE_USER")
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
     * Запрос: POST /bookings с autoSelect=false и roomId=5.
     * Ожидаемый результат: HTTP 201 Created с DTO бронирования.
     */
    @Test
    void shouldCreateBookingWithExplicitRoomId() throws Exception {
        BookingResponse response = createBookingResponse(1L, 5L, BookingStatus.CONFIRMED);

        when(bookingService.createBooking(org.mockito.ArgumentMatchers.any(BookingCreateRequest.class), anyLong()))
                .thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":5,\"startDate\":\"2026-12-01T14:00:00\","
                                + "\"endDate\":\"2026-12-05T11:00:00\",\"autoSelect\":false}")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomId").value(5))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== BC-2: Создание бронирования с автоподбором ====================

    /**
     * Проверяет успешное создание бронирования с автоподбором номера.
     * Запрос: POST /bookings с autoSelect=true.
     * Ожидаемый результат: HTTP 201 Created с DTO бронирования.
     */
    @Test
    void shouldCreateBookingWithAutoSelect() throws Exception {
        BookingResponse response = createBookingResponse(1L, 10L, BookingStatus.CONFIRMED);

        when(bookingService.createBooking(org.mockito.ArgumentMatchers.any(BookingCreateRequest.class), anyLong()))
                .thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":10,\"startDate\":\"2026-12-01T14:00:00\","
                                + "\"endDate\":\"2026-12-05T11:00:00\",\"autoSelect\":true}")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomId").value(10))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== BC-3: Получение бронирования по ID ====================

    /**
     * Проверяет успешное получение бронирования по уникальному идентификатору.
     * Запрос: GET /bookings/1.
     * Ожидаемый результат: HTTP 200 OK с DTO бронирования.
     */
    @Test
    void shouldGetBookingById() throws Exception {
        BookingResponse response = createBookingResponse(1L, 5L, BookingStatus.CONFIRMED);

        when(bookingService.getBookingById(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/bookings/1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ==================== BC-4: Получение истории бронирований ====================

    /**
     * Проверяет успешное получение истории бронирований текущего пользователя.
     * Запрос: GET /bookings/bookings.
     * Ожидаемый результат: HTTP 200 OK со списком бронирований.
     */
    @Test
    void shouldGetBookingHistory() throws Exception {
        List<BookingResponse> bookings = List.of(
                createBookingResponse(1L, 5L, BookingStatus.CONFIRMED),
                createBookingResponse(2L, 7L, BookingStatus.CANCELLED)
        );

        when(bookingService.getBookingsByUserId(anyLong())).thenReturn(bookings);

        mockMvc.perform(get("/bookings/bookings")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    // ==================== BC-5: Отмена бронирования ====================

    /**
     * Проверяет успешную отмену бронирования.
     * Запрос: DELETE /bookings/1.
     * Ожидаемый результат: HTTP 204 No Content.
     */
    @Test
    void shouldCancelBooking() throws Exception {
        mockMvc.perform(delete("/bookings/1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
    }

    // ==================== BC-6: Бронирование не найдено ====================

    /**
     * Проверяет, что запрос несуществующего бронирования возвращает 404.
     * Запрос: GET /bookings/999.
     * Ожидаемый результат: HTTP 404 Not Found с сообщением об ошибке.
     */
    @Test
    void shouldReturnNotFoundForMissingBooking() throws Exception {
        when(bookingService.getBookingById(anyLong(), anyLong()))
                .thenThrow(new com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException(
                        "Бронирование", 999L));

        mockMvc.perform(get("/bookings/999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ==================== BC-7: Доступ без токена ====================

    /**
     * Проверяет, что доступ к бронированиям без JWT-токена возвращает 401.
     * Запрос: GET /bookings/1 без заголовка Authorization.
     * Ожидаемый результат: HTTP 401 Unauthorized.
     */
    @Test
    void shouldRejectAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== BC-8: Создание бронирования без токена ====================

    /**
     * Проверяет, что создание бронирования без JWT-токена возвращает 401.
     * Запрос: POST /bookings без заголовка Authorization.
     * Ожидаемый результат: HTTP 401 Unauthorized.
     */
    @Test
    void shouldRejectBookingCreationWithoutToken() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":5,\"startDate\":\"2026-12-01T14:00:00\","
                                + "\"endDate\":\"2026-12-05T11:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }
}
