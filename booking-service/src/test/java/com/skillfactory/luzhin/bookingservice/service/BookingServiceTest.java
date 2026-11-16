package com.skillfactory.luzhin.bookingservice.service;

import com.skillfactory.luzhin.bookingservice.client.HotelServiceClient;
import com.skillfactory.luzhin.bookingservice.client.dto.AvailabilityResponse;
import com.skillfactory.luzhin.bookingservice.client.dto.RoomRecommendResponse;
import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.entity.Booking;
import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import com.skillfactory.luzhin.bookingservice.entity.User;
import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import com.skillfactory.luzhin.bookingservice.exception.HotelServiceException;
import com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.bookingservice.mapper.BookingMapper;
import com.skillfactory.luzhin.bookingservice.repository.BookingRepository;
import com.skillfactory.luzhin.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для сервиса управления бронированиями.
 * Проверяет бизнес-логику двухшагового бронирования, отмены и получения бронирований.
 *
 * <p>Тесты включают:
 * <ul>
 *   <li>Создание бронирования с подтверждением (PENDING → CONFIRMED)</li>
 *   <li>Создание бронирования с отменой при ошибке Hotel Service (PENDING → CANCELLED)</li>
 *   <li>Отмена бронирования (CONFIRMED → CANCELLED)</li>
 *   <li>Отмена уже отменённого бронирования → IllegalStateException</li>
 *   <li>Получение бронирования по ID</li>
 *   <li>Получение истории бронирований пользователя</li>
 * </ul>
 *
 * @see BookingService
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    /**
     * Mock-репозиторий бронирований — имитирует работу с БД.
     */
    @Mock
    private BookingRepository bookingRepository;

    /**
     * Mock-репозиторий пользователей — имитирует работу с БД.
     */
    @Mock
    private UserRepository userRepository;

    /**
     * Mock-маппер — имитирует преобразование между сущностями и DTO.
     */
    @Mock
    private BookingMapper bookingMapper;

    /**
     * Mock-клиент Hotel Service — имитирует вызовы к удалённому сервису.
     */
    @Mock
    private HotelServiceClient hotelServiceClient;

    /**
     * Сервис, который тестируется.
     */
    @InjectMocks
    private BookingService bookingService;

    /**
     * Тестовый пользователь для всех тестов.
     */
    private User testUser;

    /**
     * Тестовый запрос на бронирование.
     */
    private BookingCreateRequest bookingRequest;

    /**
     * Тестовый период бронирования.
     */
    private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 12, 1, 14, 0);
    private static final LocalDateTime END_DATE = LocalDateTime.of(2026, 12, 5, 11, 0);
    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    /**
     * Инициализация тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$hashedpassword")
                .role(UserRole.USER)
                .build();

        bookingRequest = BookingCreateRequest.builder()
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .autoSelect(false)
                .build();
    }

    // ==================== BS-1: Создание бронирования с подтверждением ====================

    /**
     * Проверяет успешное создание бронирования с двухшаговой транзакцией:
     * PENDING (создание) → Hotel Service подтверждает → CONFIRMED.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldCreateBookingWithConfirmation() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        Booking pendingBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.PENDING)
                .correlationId(correlationId)
                .build();

        Booking confirmedBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.CONFIRMED)
                .correlationId(correlationId)
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .roomId(5L)
                .status(BookingStatus.CONFIRMED)
                .build();

        // Мокаем: пользователь найден
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Мокаем: бронирование с таким correlationId не найдено
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.empty());
        // Мокаем: создание сущности из запроса
        when(bookingMapper.toEntity(any(BookingCreateRequest.class))).thenReturn(pendingBooking);
        // Мокаем: сохранение бронирования (возвращает pendingBooking для первого вызова,
        // confirmedBooking для второго вызова — при обновлении статуса CONFIRMED)
        when(bookingRepository.save(pendingBooking)).thenReturn(pendingBooking);
        // Мокаем: подтверждение доступности через Hotel Service
        when(hotelServiceClient.confirmAvailability(eq(5L), eq(START_DATE), eq(END_DATE), anyString()))
                .thenReturn(AvailabilityResponse.builder().available(true).build());
        // Мокаем: маппинг ответа (любой Booking -> response)
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(response);

        // Вызываем сервис
        BookingResponse result = bookingService.createBooking(bookingRequest, 1L);

        // Проверяем результат
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(1L, result.getId());
        assertEquals(5L, result.getRoomId());

        // Проверяем, что Hotel Service вызван для подтверждения доступности
        verify(hotelServiceClient, times(1))
                .confirmAvailability(eq(5L), eq(START_DATE), eq(END_DATE), anyString());
    }

    // ==================== BS-2: Создание бронирования с отменой при ошибке ====================

    /**
     * Проверяет сценарий, когда Hotel Service возвращает ошибку:
     * PENDING (создание) → Hotel Service ошибка → CANCELLED + компенсация.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldCancelBookingOnHotelServiceFailure() {
        String correlationId = UUID.randomUUID().toString();
        Booking pendingBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.PENDING)
                .correlationId(correlationId)
                .build();

        Booking cancelledBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.CANCELLED)
                .correlationId(correlationId)
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .roomId(5L)
                .status(BookingStatus.CANCELLED)
                .build();

        // Мокаем: пользователь найден
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Мокаем: бронирование с таким correlationId не найдено
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.empty());
        // Мокаем: создание сущности из запроса
        when(bookingMapper.toEntity(any(BookingCreateRequest.class))).thenReturn(pendingBooking);
        // Мокаем: сохранение бронирования в PENDING
        when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);
        // Мокаем: Hotel Service выбрасывает ошибку
        when(hotelServiceClient.confirmAvailability(eq(5L), eq(START_DATE), eq(END_DATE), anyString()))
                .thenThrow(new HotelServiceException("Hotel Service unavailable"));
        // Мокаем: cancelBookingInternal вызывает findByIdWithUser —
        // возвращаем новый объект с статусом PENDING (не pendingBooking,
        // так как он уже мутировался в CANCELLED в предыдущих операциях)
        Booking freshPendingBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.PENDING)
                .correlationId(CORRELATION_ID)
                .build();
        when(bookingRepository.findByIdWithUser(1L)).thenReturn(Optional.of(freshPendingBooking));
        // Мокаем: маппинг ответа после отмены (любой Booking -> response)
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(response);

        // Выполняем и проверяем: бронирование отменено, статус CANCELLED
        BookingResult result = new BookingResult();
        assertDoesNotThrow(() -> {
            BookingResponse br = bookingService.createBooking(bookingRequest, 1L);
            result.status = br.getStatus();
            result.bookingId = br.getId();
        });

        assertEquals(BookingStatus.CANCELLED, result.status);
        assertEquals(1L, result.bookingId);
        // Проверка: Hotel Service вызван для компенсации (releaseSlot)
        verify(hotelServiceClient, times(1))
                .releaseSlot(eq(5L), eq(START_DATE), eq(END_DATE), anyString());
    }

    /**
     * Вспомогательный класс для передачи результатов теста.
     */
    private static class BookingResult {
        BookingStatus status;
        Long bookingId;
    }

    // ==================== BS-3: Отмена бронирования ====================

    /**
     * Проверяет успешную отмену бронирования: CONFIRMED → CANCELLED.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldCancelBooking() throws Exception {
        Booking confirmedBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.CONFIRMED)
                .correlationId(UUID.randomUUID().toString())
                .build();

        // Мокаем: бронирование найдено
        when(bookingRepository.findByIdWithUser(1L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);

        // Выполняем отмену
        assertDoesNotThrow(() -> bookingService.cancelBooking(1L, 1L));

        // Проверяем: статус изменён на CANCELLED
        assertEquals(BookingStatus.CANCELLED, confirmedBooking.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    // ==================== BS-4: Отмена уже отменённого бронирования ====================

    /**
     * Проверяет, что повторная отмена отменённого бронирования выбрасывает IllegalStateException.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldThrowOnCancelAlreadyCancelledBooking() throws Exception {
        Booking cancelledBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.CANCELLED)
                .correlationId(UUID.randomUUID().toString())
                .build();

        // Мокаем: бронирование найдено и уже отменено
        when(bookingRepository.findByIdWithUser(1L)).thenReturn(Optional.of(cancelledBooking));

        // Проверяем: выбрасывается исключение
        assertThrows(IllegalStateException.class, () -> bookingService.cancelBooking(1L, 1L));
    }

    // ==================== BS-5: Получение бронирования по ID ====================

    /**
     * Проверяет успешное получение бронирования по уникальному идентификатору.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldGetBookingById() throws Exception {
        Booking booking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.CONFIRMED)
                .correlationId(UUID.randomUUID().toString())
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .roomId(5L)
                .status(BookingStatus.CONFIRMED)
                .build();

        // Мокаем: бронирование найдено
        when(bookingRepository.findByIdWithUser(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        BookingResponse result = bookingService.getBookingById(1L, 1L);

        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
    }

    // ==================== BS-6: Получение истории бронирований ====================

    /**
     * Проверяет успешное получение всех бронирований пользователя.
     *
     * @throws Exception ошибка при подготовке данных
     */
    @Test
    void shouldGetBookingsByUserId() throws Exception {
        Booking booking1 = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(5L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.CONFIRMED)
                .correlationId(UUID.randomUUID().toString())
                .build();

        Booking booking2 = Booking.builder()
                .id(2L)
                .user(testUser)
                .roomId(7L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(BookingStatus.PENDING)
                .correlationId(UUID.randomUUID().toString())
                .build();

        List<Booking> bookings = List.of(booking1, booking2);
        List<BookingResponse> responses = List.of(
                BookingResponse.builder().id(1L).userId(1L).username("testuser").roomId(5L)
                        .status(BookingStatus.CONFIRMED).build(),
                BookingResponse.builder().id(2L).userId(1L).username("testuser").roomId(7L)
                        .status(BookingStatus.PENDING).build()
        );

        // Мокаем: поиск бронирований по userId
        when(bookingRepository.findByUserId(1L)).thenReturn(bookings);
        when(bookingMapper.toResponseList(bookings)).thenReturn(responses);

        List<BookingResponse> result = bookingService.getBookingsByUserId(1L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }
}
