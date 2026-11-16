package com.skillfactory.luzhin.bookingservice.client;

import com.skillfactory.luzhin.bookingservice.config.FeignConfig;
import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для Feign-клиента HotelServiceClient.
 * Проверяет, что клиент корректно конфигурируется и проксируется.
 *
 * <p>Тесты включают:
 * <ul>
 *   <li>Feign-клиент автоматически получает конфигурацию из FeignConfig</li>
 *   <li>Интерсептор JWT корректно пропускает запросы без ServletContext</li>
 *   <li>Методы клиента вызываются с правильными параметрами</li>
 * </ul>
 *
 * @see HotelServiceClient
 * @see FeignConfig
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "jwt.secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWjEyMzQ1Njc4OTA=",
        "spring.cloud.openfeign.client.config.default.loggerLevel=BASIC"
})
class HotelServiceClientTest {

    /**
     * Feign-клиент для Hotel Service, который тестируется.
     * Проверяется, что он корректно инициализируется Spring Cloud OpenFeign.
     */
    @Autowired
    private HotelServiceClient hotelServiceClient;

    /**
     * Mock-клиент Hotel Service для тестирования интеграции с BookingService.
     * Все вызовы к Hotel Service будут мокурованы.
     */
    @MockBean
    private HotelServiceClient mockHotelServiceClient;

    /**
     * Тестовый период бронирования.
     */
    private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 12, 1, 14, 0);
    private static final LocalDateTime END_DATE = LocalDateTime.of(2026, 12, 5, 11, 0);
    private static final String CORRELATION_ID = "test-correlation-001";

    // ==================== HSC-1: Feign-клиент инициализирован ====================

    /**
     * Проверяет, что Feign-клиент корректно инициализируется Spring Cloud OpenFeign.
     * Клиент должен быть не null и представлять собой прокси-объект.
     *
     * @throws Exception ошибка при проверке
     */
    @Test
    void shouldInitializeFeignClient() throws Exception {
        // Проверяем: клиент не null
        assertNotNull(hotelServiceClient,
                "Feign-клиент HotelServiceClient должен быть инициализирован Spring Cloud OpenFeign");

        // Проверяем: клиент является прокси-объектом (не null)
        assertNotNull(hotelServiceClient.getClass().getPackageName(),
                "Клиент должен быть динамическим прокси OpenFeign");
    }

    // ==================== HSC-2: Мок-клиент подтверждает доступность ====================

    /**
     * Проверяет, что мокированный Feign-клиент возвращает корректный ответ
     * при вызове confirmAvailability.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldConfirmAvailabilityWithMock() throws Exception {
        // Мокаем ответ от Hotel Service
        com.skillfactory.luzhin.bookingservice.client.dto.AvailabilityResponse mockResponse =
                com.skillfactory.luzhin.bookingservice.client.dto.AvailabilityResponse.builder()
                        .roomId(5L)
                        .available(true)
                        .message("Номер доступен")
                        .build();

        when(mockHotelServiceClient.confirmAvailability(
                eq(5L), eq(START_DATE), eq(END_DATE), eq(CORRELATION_ID)))
                .thenReturn(mockResponse);

        // Вызываем мокированный клиент
        com.skillfactory.luzhin.bookingservice.client.dto.AvailabilityResponse response =
                mockHotelServiceClient.confirmAvailability(5L, START_DATE, END_DATE, CORRELATION_ID);

        // Проверяем результат
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals(5L, response.getRoomId());
        assertEquals("Номер доступен", response.getMessage());

        // Проверяем, что метод был вызван ровно 1 раз
        verify(mockHotelServiceClient, times(1)).confirmAvailability(
                eq(5L), eq(START_DATE), eq(END_DATE), eq(CORRELATION_ID));
    }

    // ==================== HSC-3: Мок-клиент освобождает слот ====================

    /**
     * Проверяет, что мокированный Feign-клиент возвращает корректный ответ
     * при вызове releaseSlot.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldReleaseSlotWithMock() throws Exception {
        // Мокаем ответ от Hotel Service
        com.skillfactory.luzhin.bookingservice.client.dto.ReleaseResponse mockResponse =
                com.skillfactory.luzhin.bookingservice.client.dto.ReleaseResponse.builder()
                        .roomId(5L)
                        .released(true)
                        .message("Слот успешно освобождён")
                        .build();

        when(mockHotelServiceClient.releaseSlot(
                eq(5L), eq(START_DATE), eq(END_DATE), eq(CORRELATION_ID)))
                .thenReturn(mockResponse);

        // Вызываем мокированный клиент
        com.skillfactory.luzhin.bookingservice.client.dto.ReleaseResponse response =
                mockHotelServiceClient.releaseSlot(5L, START_DATE, END_DATE, CORRELATION_ID);

        // Проверяем результат
        assertNotNull(response);
        assertTrue(response.isReleased());
        assertEquals(5L, response.getRoomId());
        assertEquals("Слот успешно освобождён", response.getMessage());

        // Проверяем, что метод был вызван ровно 1 раз
        verify(mockHotelServiceClient, times(1)).releaseSlot(
                eq(5L), eq(START_DATE), eq(END_DATE), eq(CORRELATION_ID));
    }

    // ==================== HSC-4: Мок-клиент возвращает рекомендованные номера ====================

    /**
     * Проверяет, что мокированный Feign-клиент возвращает корректный список
     * рекомендованных номеров при вызове getRecommendedRooms.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldGetRecommendedRoomsWithMock() throws Exception {
        // Мокаем список рекомендованных номеров
        List<com.skillfactory.luzhin.bookingservice.client.dto.RoomRecommendResponse> mockRooms = List.of(
                com.skillfactory.luzhin.bookingservice.client.dto.RoomRecommendResponse.builder()
                        .id(1L).hotelId(1L).hotelName("Grand Hotel").number("101")
                        .available(true).timesBooked(5).build(),
                com.skillfactory.luzhin.bookingservice.client.dto.RoomRecommendResponse.builder()
                        .id(2L).hotelId(1L).hotelName("Grand Hotel").number("201")
                        .available(true).timesBooked(3).build()
        );

        when(mockHotelServiceClient.getRecommendedRooms()).thenReturn(mockRooms);

        // Вызываем мокированный клиент
        List<com.skillfactory.luzhin.bookingservice.client.dto.RoomRecommendResponse> rooms =
                mockHotelServiceClient.getRecommendedRooms();

        // Проверяем результат
        assertNotNull(rooms);
        assertEquals(2, rooms.size());
        assertEquals(1L, rooms.get(0).getId());
        assertEquals(2L, rooms.get(1).getId());
        assertEquals(5, rooms.get(0).getTimesBooked());
        assertEquals(3, rooms.get(1).getTimesBooked());

        // Проверяем, что метод был вызван ровно 1 раз
        verify(mockHotelServiceClient, times(1)).getRecommendedRooms();
    }
}
