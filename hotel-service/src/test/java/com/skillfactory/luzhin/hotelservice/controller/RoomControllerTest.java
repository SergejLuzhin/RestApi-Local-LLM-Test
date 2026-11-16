package com.skillfactory.luzhin.hotelservice.controller;

import com.skillfactory.luzhin.hotelservice.dto.HotelResponse;
import com.skillfactory.luzhin.hotelservice.dto.RoomCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.RoomResponse;
import com.skillfactory.luzhin.hotelservice.service.RoomService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для REST-контроллера управления номерами отелей.
 * Проверяет CRUD-операции для номеров, а также эндпоинты подтверждения доступности
 * и снятия временной блокировки.
 *
 * <p>Тесты включают проверки авторизации:
 * <ul>
 *   <li>Создание номера требует роль ADMIN</li>
 *   <li>Публичные GET-эндпоинты и внутренние эндпоинты доступны без аутентификации</li>
 * </ul>
 *
 * @see RoomController
 * @see RoomService
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWjEyMzQ1Njc4OTA="
})
class RoomControllerTest {

    /**
     * Mock для сервиса номеров — все вызовы бизнес-логики мокнуты.
     */
    @MockBean
    private RoomService roomService;

    /**
     * Секретный ключ JWT из конфигурации. Используется для генерации
     * валидных тестовых токенов с теми же параметрами, что и в production.
     */
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
     * Создает JWT-токен для указанного пользователя и роли.
     *
     * @param username имя пользователя
     * @param role роль пользователя (USER или ADMIN)
     * @return JWT-токен в строковом формате
     */
    private String generateToken(String username, String role) {
        String roleClaim = "ROLE_" + role.toUpperCase();
        return Jwts.builder()
                .subject(username)
                .claim("role", roleClaim)
                .expiration(java.util.Date.from(
                        java.time.Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
    }

    // ==================== H-RT-1: Создание номера (успех) ====================

    /**
     * Проверяет успешное создание номера с ролью ADMIN и валидным hotelId.
     * Запрос: POST /api/rooms с валидными данными.
     * Ожидаемый результат: HTTP 201 Created с DTO созданного номера.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldCreateRoomWithAdminRole() throws Exception {
        RoomResponse response = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        when(roomService.createRoom(any(RoomCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":1,\"number\":\"101\",\"available\":true}")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hotelId").value(1))
                .andExpect(jsonPath("$.hotelName").value("Grand Hotel"))
                .andExpect(jsonPath("$.number").value("101"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.timesBooked").value(0));
    }

    // ==================== H-RT-2: Создание номера без токена ====================

    /**
     * Проверяет, что создание номера без JWT-токена возвращает 401 Unauthorized.
     * Эндпоинт POST /api/rooms защищён @PreAuthorize("hasRole('ADMIN')").
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectRoomCreationWithoutToken() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":1,\"number\":\"101\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== H-RT-3: Создание номера с несуществующим отелем ====================

    /**
     * Проверяет, что создание номера с несуществующим hotelId возвращает 404 Not Found.
     * Запрос: POST /api/rooms с hotelId=999, который не существует.
     * Ожидаемый результат: HTTP 404 Not Found.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectRoomCreationWithMissingHotel() throws Exception {
        doThrow(new com.skillfactory.luzhin.hotelservice.exception.ResourceNotFoundException("Hotel", 999L))
                .when(roomService).createRoom(any(RoomCreateRequest.class));

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":999,\"number\":\"202\"}")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ==================== H-RT-4: Создание номера без hotelId (валидация) ====================

    /**
     * Проверяет валидацию входных данных при создании номера.
     * Запрос: POST /api/rooms с отсутствующим hotelId (null).
     * Ожидаемый результат: HTTP 400 Bad Request с ошибкой валидации по полю hotelId.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectRoomCreationWithoutHotelId() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"101\"}")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.hotelId").exists());
    }

    // ==================== H-RT-5: Получение доступных номеров ====================

    /**
     * Проверяет получение списка доступных номеров без токена (публичный эндпоинт).
     * Эндпоинт GET /api/rooms доступен без аутентификации согласно SecurityConfig.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldGetAvailableRoomsWithoutToken() throws Exception {
        RoomResponse room1 = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();
        RoomResponse room2 = RoomResponse.builder()
                .id(2L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("102")
                .available(true)
                .timesBooked(0)
                .build();

        when(roomService.getAllAvailableRooms()).thenReturn(List.of(room1, room2));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value("101"))
                .andExpect(jsonPath("$[1].number").value("102"));
    }

    // ==================== H-RT-6: Рекомендованные номера ====================

    /**
     * Проверяет получение рекомендованных номеров (публичный эндпоинт).
     * Сортировка по timesBooked ASC, id ASC обеспечивается сервисом.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldGetRecommendedRoomsWithoutToken() throws Exception {
        RoomResponse room1 = RoomResponse.builder()
                .id(2L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("102")
                .available(true)
                .timesBooked(0)
                .build();
        RoomResponse room2 = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("101")
                .available(true)
                .timesBooked(1)
                .build();

        when(roomService.getRecommendedRooms()).thenReturn(List.of(room1, room2));

        mockMvc.perform(get("/api/rooms/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Первый номер с меньшим timesBooked
                .andExpect(jsonPath("$[0].timesBooked").value(0))
                // Второй номер с большим timesBooked
                .andExpect(jsonPath("$[1].timesBooked").value(1));
    }

    // ==================== H-RT-7: Подтверждение доступности ====================

    /**
     * Проверяет успешное подтверждение доступности номера на запрошенные даты.
     * Нет пересекающихся бронирований — слот блокируется.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldConfirmAvailabilitySuccessfully() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);

        doNothing().when(roomService).confirmAvailability(eq(1L), eq(start), eq(end), anyString());

        mockMvc.perform(post("/api/rooms/1/confirm-availability")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .param("correlationId", "test-correlation-001")
                        .header("Authorization", "Bearer " + generateToken("system", "ADMIN")))
                .andExpect(status().isOk());
    }

    // ==================== H-RT-8: Подтверждение доступности — пересечение ====================

    /**
     * Проверяет, что подтверждение доступности при наличии пересекающегося
     * бронирования возвращает 409 Conflict.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectAvailabilityConfirmationOnOverlap() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);

        doThrow(new com.skillfactory.luzhin.hotelservice.exception.RoomNotAvailableException(
                        1L, "Room is already booked for the specified dates"))
                .when(roomService).confirmAvailability(eq(1L), eq(start), eq(end), anyString());

        mockMvc.perform(post("/api/rooms/1/confirm-availability")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .param("correlationId", "test-correlation-002")
                        .header("Authorization", "Bearer " + generateToken("system", "ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // ==================== H-RT-9: Снятие блокировки ====================

    /**
     * Проверяет успешное снятие временной блокировки слота.
     * Блокировка существует — releaseSlot выполняется успешно.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldReleaseSlotSuccessfully() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);

        doNothing().when(roomService).releaseSlot(eq(1L), eq(start), eq(end), anyString());

        mockMvc.perform(post("/api/rooms/1/release")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .param("correlationId", "test-correlation-003")
                        .header("Authorization", "Bearer " + generateToken("system", "ADMIN")))
                .andExpect(status().isOk());
    }

    // ==================== H-RT-10: Release без активной блокировки ====================

    /**
     * Проверяет, что снятие блокировки при отсутствии активной блокировки
     * возвращает 200 OK (пустой результат, без ошибок).
     * releaseSlot обрабатывает отсутствие блокировок корректно.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldReleaseSlotSuccessfullyWhenNoSlotExists() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);

        // releaseSlot не выбрасывает исключение при отсутствии блокировки
        doNothing().when(roomService).releaseSlot(eq(1L), eq(start), eq(end), anyString());

        mockMvc.perform(post("/api/rooms/1/release")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .param("correlationId", "test-correlation-004")
                        .header("Authorization", "Bearer " + generateToken("system", "ADMIN")))
                .andExpect(status().isOk());
    }
}
