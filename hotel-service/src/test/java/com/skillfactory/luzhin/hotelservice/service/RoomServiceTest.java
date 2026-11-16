package com.skillfactory.luzhin.hotelservice.service;

import com.skillfactory.luzhin.hotelservice.dto.RoomCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.RoomResponse;
import com.skillfactory.luzhin.hotelservice.entity.Booking;
import com.skillfactory.luzhin.hotelservice.entity.Hotel;
import com.skillfactory.luzhin.hotelservice.entity.Room;
import com.skillfactory.luzhin.hotelservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.hotelservice.exception.RoomNotAvailableException;
import com.skillfactory.luzhin.hotelservice.mapper.RoomMapper;
import com.skillfactory.luzhin.hotelservice.repository.BookingRepository;
import com.skillfactory.luzhin.hotelservice.repository.HotelRepository;
import com.skillfactory.luzhin.hotelservice.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для сервиса управления номерами отеля.
 * Проверяет бизнес-логику без загрузки Spring-контекста.
 *
 * <p>Тесты охватывают CRUD-операции и методы подтверждения/снятия доступности:
 * <ul>
 *   <li>Создание номера с проверкой существования отеля</li>
 *   <li>Получение доступных и рекомендованных номеров</li>
 *   <li>Подтверждение доступности (успех, пересечение, номер не найден)</li>
 *   <li>Снятие временной блокировки</li>
 * </ul>
 *
 * @see RoomService
 * @see RoomRepository
 * @see HotelRepository
 * @see BookingRepository
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    /**
     * Моки для репозиториев.
     */
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private BookingRepository bookingRepository;

    /**
     * Моки для MapStruct-маппера.
     */
    @Mock
    private RoomMapper roomMapper;

    /**
     * Сервис, тестируемый в данном тестовом классе.
     * Инъекция моков через конструктор.
     */
    @InjectMocks
    private RoomService roomService;

    /**
     * Тестируемый hotel — создаётся в каждом тесте отдельно.
     */
    private Hotel testHotel;

    /**
     * Тестируемый room — создаётся в каждом тесте отдельно.
     */
    private Room testRoom;

    /**
     * Инициализация тестовых данных, общих для большинства тестов.
     */
    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .build();

        testRoom = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();
    }

    // ==================== H-RS-1: Создание номера ====================

    /**
     * Проверяет успешное создание номера.
     * Отель существует, RoomRepository.save() вызывается один раз.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldCreateRoomSuccessfully() {
        // Arrange
        RoomCreateRequest request = RoomCreateRequest.builder()
                .hotelId(1L)
                .number("101")
                .available(true)
                .build();

        Room savedRoom = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        RoomResponse expectedResponse = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(roomMapper.toEntity(request)).thenReturn(savedRoom);
        when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);
        when(roomMapper.toResponse(savedRoom)).thenReturn(expectedResponse);

        // Act
        RoomResponse result = roomService.createRoom(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("101", result.getNumber());
        assertEquals("Grand Hotel", result.getHotelName());
        assertTrue(result.getAvailable());

        // Verify — hotelRepository.findById вызван 1 раз, roomRepository.save вызван 1 раз
        verify(hotelRepository, times(1)).findById(1L);
        verify(roomRepository, times(1)).save(any(Room.class));
        verify(roomMapper, times(1)).toEntity(request);
        verify(roomMapper, times(1)).toResponse(savedRoom);
    }

    // ==================== H-RS-2: Получение всех доступных номеров ====================

    /**
     * Проверяет получение списка всех доступных номеров.
     * RoomRepository.findByAvailableTrue() возвращает список номеров.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldGetAllAvailableRooms() {
        // Arrange
        Room room1 = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        Room room2 = Room.builder()
                .id(2L)
                .hotel(testHotel)
                .number("102")
                .available(true)
                .timesBooked(1)
                .build();

        RoomResponse response1 = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();

        RoomResponse response2 = RoomResponse.builder()
                .id(2L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("102")
                .available(true)
                .timesBooked(1)
                .build();

        when(roomRepository.findByAvailableTrue()).thenReturn(List.of(room1, room2));
        when(roomMapper.toResponseList(List.of(room1, room2))).thenReturn(List.of(response1, response2));

        // Act
        List<RoomResponse> result = roomService.getAllAvailableRooms();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getAvailable());
        assertTrue(result.get(1).getAvailable());

        // Verify
        verify(roomRepository, times(1)).findByAvailableTrue();
        verify(roomMapper, times(1)).toResponseList(List.of(room1, room2));
    }

    // ==================== H-RS-3: Получение рекомендованных номеров ====================

    /**
     * Проверяет получение рекомендованных номеров, отсортированных по алгоритму
     * равномерной загрузки: timesBooked ASC, id ASC.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldGetRecommendedRoomsSortedByTimesBooked() {
        // Arrange — номера с разным timesBooked, сортировка ожидается по возрастанию
        Room roomWithZeroBookings = Room.builder()
                .id(2L)
                .hotel(testHotel)
                .number("102")
                .available(true)
                .timesBooked(0)
                .build();

        Room roomWithOneBooking = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(1)
                .build();

        RoomResponse responseZero = RoomResponse.builder()
                .id(2L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("102")
                .available(true)
                .timesBooked(0)
                .build();

        RoomResponse responseOne = RoomResponse.builder()
                .id(1L)
                .hotelId(1L)
                .hotelName("Grand Hotel")
                .number("101")
                .available(true)
                .timesBooked(1)
                .build();

        when(roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc())
                .thenReturn(List.of(roomWithZeroBookings, roomWithOneBooking));
        when(roomMapper.toResponseList(List.of(roomWithZeroBookings, roomWithOneBooking)))
                .thenReturn(List.of(responseZero, responseOne));

        // Act
        List<RoomResponse> result = roomService.getRecommendedRooms();

        // Assert — первый номер имеет меньший timesBooked
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getTimesBooked());
        assertEquals(1, result.get(1).getTimesBooked());

        // Verify
        verify(roomRepository, times(1)).findByAvailableTrueOrderByTimesBookedAscIdAsc();
    }

    // ==================== H-RS-4: Подтверждение доступности — успешно ====================

    /**
     * Проверяет успешное подтверждение доступности номера.
     * Нет пересекающихся бронирований — временное бронирование создаётся.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldConfirmAvailabilitySuccessfully() {
        // Arrange
        Long roomId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);
        String correlationId = "test-correlation-001";

        when(roomRepository.findByIdWithHotel(roomId)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findOverlappingBookings(roomId, start, end)).thenReturn(List.of());

        // Act — не должно выбросить исключение
        assertDoesNotThrow(() -> roomService.confirmAvailability(roomId, start, end, correlationId));

        // Verify — номер найден, пересечений нет, временное бронирование создано
        verify(roomRepository, times(1)).findByIdWithHotel(roomId);
        verify(bookingRepository, times(1)).findOverlappingBookings(roomId, start, end);

        // Проверяем, что save был вызван (создание временного бронирования)
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    // ==================== H-RS-5: Подтверждение доступности — занято ====================

    /**
     * Проверяет, что при наличии пересекающегося бронирования выбрасывается
     * RoomNotAvailableException.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldThrowExceptionWhenRoomIsBooked() {
        // Arrange — есть пересекающееся бронирование
        Long roomId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);
        String correlationId = "test-correlation-002";

        Booking overlappingBooking = Booking.builder()
                .id(10L)
                .userId(5L)
                .roomId(roomId)
                .startDate(LocalDateTime.of(2026, 12, 3, 10, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 7, 11, 0, 0))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(roomRepository.findByIdWithHotel(roomId)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findOverlappingBookings(roomId, start, end))
                .thenReturn(List.of(overlappingBooking));

        // Act & Assert — должно выбросить RoomNotAvailableException
        RoomNotAvailableException exception = assertThrows(
                RoomNotAvailableException.class,
                () -> roomService.confirmAvailability(roomId, start, end, correlationId)
        );

        assertTrue(exception.getMessage().contains("1"));
        assertTrue(exception.getMessage().contains("already booked"));

        // Verify — save не вызывался (блокировка не создаётся при конфликте)
        verify(roomRepository, times(1)).findByIdWithHotel(roomId);
        verify(bookingRepository, times(1)).findOverlappingBookings(roomId, start, end);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ==================== H-RS-6: Подтверждение доступности — номер не найден ====================

    /**
     * Проверяет, что при несуществующем номере выбрасывается ResourceNotFoundException.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldThrowExceptionWhenRoomNotFoundForAvailability() {
        // Arrange
        Long roomId = 999L;
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);
        String correlationId = "test-correlation-003";

        when(roomRepository.findByIdWithHotel(roomId)).thenReturn(Optional.empty());

        // Act & Assert — должно выбросить ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> roomService.confirmAvailability(roomId, start, end, correlationId)
        );

        assertTrue(exception.getMessage().contains("Room"));
        assertTrue(exception.getMessage().contains("999"));

        // Verify — save и findOverlappingBookings не вызывались
        verify(roomRepository, times(1)).findByIdWithHotel(roomId);
        verify(bookingRepository, never()).findOverlappingBookings(anyLong(), any(), any());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ==================== H-RS-7: Снятие блокировки ====================

    /**
     * Проверяет успешное снятие временной блокировки слота.
     * BookingRepository.deleteTemporaryBookings() вызывается.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldReleaseSlotSuccessfully() {
        // Arrange
        Long roomId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);
        String correlationId = "test-correlation-004";

        when(bookingRepository.deleteTemporaryBookings(roomId, start, end)).thenReturn(1);

        // Act — не должно выбросить исключение
        assertDoesNotThrow(() -> roomService.releaseSlot(roomId, start, end, correlationId));

        // Verify
        verify(bookingRepository, times(1)).deleteTemporaryBookings(roomId, start, end);
    }

    // ==================== H-RS-8: Подтверждение доступности — непересекающиеся даты ====================

    /**
     * Проверяет успешное подтверждение доступности при непересекающихся датах.
     * Запрос на период, который полностью предшествует существующему бронированию.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldConfirmAvailabilityWithNonOverlappingDates() {
        // Arrange — существующее бронирование на 12.01–12.05,
        // запрос на 12.06–12.10 (полностью после, без пересечения)
        Long roomId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 12, 6, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 10, 11, 0, 0);
        String correlationId = "test-correlation-005";

        // Существующее бронирование, которое не пересекается с запросом
        Booking existingBooking = Booking.builder()
                .id(10L)
                .userId(5L)
                .roomId(roomId)
                .startDate(LocalDateTime.of(2026, 12, 1, 14, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 5, 11, 0, 0))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(roomRepository.findByIdWithHotel(roomId)).thenReturn(Optional.of(testRoom));
        // findOverlappingBookings вернёт пустой список — пересечений нет
        when(bookingRepository.findOverlappingBookings(roomId, start, end)).thenReturn(List.of());

        // Act — не должно выбросить исключение
        assertDoesNotThrow(() -> roomService.confirmAvailability(roomId, start, end, correlationId));

        // Verify — save был вызван, так как пересечений нет
        verify(roomRepository, times(1)).findByIdWithHotel(roomId);
        verify(bookingRepository, times(1)).findOverlappingBookings(roomId, start, end);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }
}
