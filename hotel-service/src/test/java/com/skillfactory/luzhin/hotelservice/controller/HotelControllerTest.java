package com.skillfactory.luzhin.hotelservice.controller;

import com.skillfactory.luzhin.hotelservice.dto.HotelCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.HotelResponse;
import com.skillfactory.luzhin.hotelservice.service.HotelService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для REST-контроллера управления отелями.
 * Проверяет CRUD-операции: создание, получение списка, получение по ID, обновление, удаление.
 *
 * <p>Тесты включают проверки авторизации:
 * <ul>
 *   <li>Доступ без токена → 401 Unauthorized</li>
 *   <li>Доступ с ролью USER к ADMIN-эндпоинтам → 403 Forbidden</li>
 *   <li>Публичные GET-эндпоинты доступны без аутентификации</li>
 * </ul>
 *
 * @see HotelController
 * @see HotelService
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWjEyMzQ1Njc4OTA="
})
class HotelControllerTest {

    /**
     * Mock для сервиса отелей — все вызовы бизнес-логики мокнуты.
     */
    @MockBean
    private HotelService hotelService;

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

    // ==================== H-CT-1: Создание отеля (успех) ====================

    /**
     * Проверяет успешное создание отеля с ролью ADMIN.
     * Запрос: POST /hotels с валидными данными.
     * Ожидаемый результат: HTTP 201 Created с DTO созданного отеля.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldCreateHotelWithAdminRole() throws Exception {
        HotelResponse response = HotelResponse.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .totalRooms(0)
                .availableRooms(0)
                .build();

        when(hotelService.createHotel(any())).thenReturn(response);

        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grand Hotel\",\"address\":\"123 Main St, New York\"}")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Grand Hotel"))
                .andExpect(jsonPath("$.address").value("123 Main St, New York"));
    }

    // ==================== H-CT-2: Валидация создания отеля ====================

    /**
     * Проверяет валидацию входных данных при создании отеля.
     * Запрос: POST /hotels с пустым телом.
     * Ожидаемый результат: HTTP 400 Bad Request с ошибками валидации.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectEmptyBodyOnCreateHotel() throws Exception {
        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.address").exists());
    }

    // ==================== H-CT-3: Создание отеля без токена ====================

    /**
     * Проверяет, что создание отеля без JWT-токена возвращает 401 Unauthorized.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectHotelCreationWithoutToken() throws Exception {
        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grand Hotel\",\"address\":\"123 Main St\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== H-CT-4: Создание отеля токеном USER ====================

    /**
     * Проверяет, что создание отеля с ролью USER возвращает 403 Forbidden.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldRejectHotelCreationWithUserRole() throws Exception {
        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grand Hotel\",\"address\":\"123 Main St\"}")
                        .header("Authorization", "Bearer " + generateToken("user", "USER")))
                .andExpect(status().isForbidden());
    }

    // ==================== H-CT-5: Получение списка отелей ====================

    /**
     * Проверяет получение списка отелей без токена (публичный эндпоинт).
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldGetHotelsListWithoutToken() throws Exception {
        when(hotelService.getAllHotels()).thenReturn(List.of());

        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== H-CT-6: Получение отеля по ID ====================

    /**
     * Проверяет получение отеля по ID с ролью USER.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldGetHotelByIdWithUserRole() throws Exception {
        HotelResponse response = HotelResponse.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St")
                .totalRooms(5)
                .availableRooms(3)
                .build();

        when(hotelService.getHotelById(1L)).thenReturn(response);

        mockMvc.perform(get("/hotels/1")
                        .header("Authorization", "Bearer " + generateToken("user", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Grand Hotel"))
                .andExpect(jsonPath("$.totalRooms").value(5))
                .andExpect(jsonPath("$.availableRooms").value(3));
    }

    // ==================== H-CT-7: Отель не найден ====================

    /**
     * Проверяет, что запрос несуществующего отеля возвращает 404 Not Found.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldReturnNotFoundForMissingHotel() throws Exception {
        when(hotelService.getHotelById(999L))
                .thenThrow(new com.skillfactory.luzhin.hotelservice.exception.ResourceNotFoundException("Hotel", 999L));

        mockMvc.perform(get("/hotels/999")
                        .header("Authorization", "Bearer " + generateToken("user", "USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ==================== H-CT-8: Обновление отеля ====================

    /**
     * Проверяет успешное обновление отеля с ролью ADMIN.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldUpdateHotelWithAdminRole() throws Exception {
        HotelResponse response = HotelResponse.builder()
                .id(1L)
                .name("Updated Hotel Name")
                .address("456 New Address")
                .totalRooms(5)
                .availableRooms(3)
                .build();

        when(hotelService.updateHotel(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Hotel Name\",\"address\":\"456 New Address\"}")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Hotel Name"))
                .andExpect(jsonPath("$.address").value("456 New Address"));
    }

    // ==================== H-CT-9: Удаление отеля ====================

    /**
     * Проверяет успешное удаление отеля с ролью ADMIN.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldDeleteHotelWithAdminRole() throws Exception {
        doNothing().when(hotelService).deleteHotel(1L);

        mockMvc.perform(delete("/hotels/1")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }

    // ==================== H-CT-10: Удаление несуществующего отеля ====================

    /**
     * Проверяет, что удаление несуществующего отеля возвращает 404 Not Found.
     *
     * @throws Exception ошибка при выполнении запроса
     */
    @Test
    void shouldReturnNotFoundWhenDeletingMissingHotel() throws Exception {
        doThrow(new com.skillfactory.luzhin.hotelservice.exception.ResourceNotFoundException("Hotel", 999L))
                .when(hotelService).deleteHotel(999L);

        mockMvc.perform(delete("/hotels/999")
                        .header("Authorization", "Bearer " + generateToken("admin", "ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
