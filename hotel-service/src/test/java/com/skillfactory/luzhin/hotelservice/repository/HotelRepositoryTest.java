package com.skillfactory.luzhin.hotelservice.repository;

import com.skillfactory.luzhin.hotelservice.entity.Booking;
import com.skillfactory.luzhin.hotelservice.entity.Hotel;
import com.skillfactory.luzhin.hotelservice.entity.Room;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для репозиториев Hotel Service.
 * Загружает Spring-контекст JPA для проверки SQL-запросов и работы с БД.
 *
 * <p>Тесты охватывают методы всех трёх репозиториев:
 * <ul>
 *   <li>HotelRepository — поиск по имени, загрузка с номерами</li>
 *   <li>RoomRepository — сортировка, проверка пересечений бронирований</li>
 *   <li>BookingRepository — поиск и удаление временных бронирований</li>
 * </ul>
 *
 * @see HotelRepository
 * @see RoomRepository
 * @see BookingRepository
 */
@DataJpaTest
class HotelRepositoryTest {

    /**
     * Тестовый менеджер сущностей для сохранения и поиска данных в тестовой БД.
     */
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Helper-метод для фиксации изменений в persistence context
     * и синхронизации с базой данных.
     */
    private void flush() {
        entityManager.flush();
    }

    // ==================== H-REPO-1: Сортировка по timesBooked ====================

    /**
     * Проверяет сортировку номеров по timesBooked ASC, id ASC.
     * Создаёт 3 номера с разным timesBooked и проверяет порядок.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldOrderByTimesBookedAscIdAsc() {
        // Arrange
        Hotel hotel = Hotel.builder()
                .name("Test Hotel")
                .address("123 Test St")
                .build();
        entityManager.persist(hotel);

        // Номера с разным timesBooked — ID не задаются явно, Hibernate сгенерирует
        Room room1 = Room.builder()
                .hotel(hotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();
        entityManager.persist(room1);

        Room room2 = Room.builder()
                .hotel(hotel)
                .number("202")
                .available(true)
                .timesBooked(0)
                .build();
        entityManager.persist(room2);

        Room room3 = Room.builder()
                .hotel(hotel)
                .number("303")
                .available(true)
                .timesBooked(1)
                .build();
        entityManager.persist(room3);

        flush();

        // Получаем сущности из БД для извлечения сгенерированных ID
        Room persistedRoom1 = entityManager.find(Room.class, room1.getId());
        Room persistedRoom2 = entityManager.find(Room.class, room2.getId());
        Room persistedRoom3 = entityManager.find(Room.class, room3.getId());

        // Act
        List<Room> result = roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc();

        // Assert — сортировка: сначала timesBooked=0 (по id ASC), затем timesBooked=1
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getTimesBooked());
        assertEquals(0, result.get(1).getTimesBooked());
        assertEquals(1, result.get(2).getTimesBooked());

        // При равенстве timesBooked сортировка по id ASC
        assertTrue(result.get(0).getId() < result.get(1).getId());
    }

    // ==================== H-REPO-2: Пересечение — есть ====================

    /**
     * Проверяет, что findOverlappingBookings находит пересечение.
     * Существующее бронирование 10:00–12:00, запрос 11:00–13:00.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldFindOverlappingBookings() {
        // Arrange
        LocalDateTime existingStart = LocalDateTime.of(2026, 12, 1, 10, 0, 0);
        LocalDateTime existingEnd = LocalDateTime.of(2026, 12, 1, 12, 0, 0);

        Booking booking = Booking.builder()
                .userId(5L)
                .roomId(1L)
                .startDate(existingStart)
                .endDate(existingEnd)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        entityManager.persist(booking);
        flush();

        Long roomId = 1L;

        // Запрос на период 11:00–13:00 (пересекается с 10:00–12:00)
        LocalDateTime queryStart = LocalDateTime.of(2026, 12, 1, 11, 0, 0);
        LocalDateTime queryEnd = LocalDateTime.of(2026, 12, 1, 13, 0, 0);

        // Act
        List<Booking> result = bookingRepository.findOverlappingBookings(roomId, queryStart, queryEnd);

        // Assert
        assertEquals(1, result.size());
    }

    // ==================== H-REPO-3: Пересечение — нет (после) ====================

    /**
     * Проверяет, что при запросе периода после существующего бронирования
     * пересечений не находится.
     * Существующее 10:00–12:00, запрос 13:00–15:00.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldNotFindOverlappingBookingsWhenAfter() {
        // Arrange
        LocalDateTime existingStart = LocalDateTime.of(2026, 12, 1, 10, 0, 0);
        LocalDateTime existingEnd = LocalDateTime.of(2026, 12, 1, 12, 0, 0);

        Booking booking = Booking.builder()
                .userId(5L)
                .roomId(1L)
                .startDate(existingStart)
                .endDate(existingEnd)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        entityManager.persist(booking);
        flush();

        // Запрос 13:00–15:00 (после существующего, без пересечения)
        LocalDateTime queryStart = LocalDateTime.of(2026, 12, 1, 13, 0, 0);
        LocalDateTime queryEnd = LocalDateTime.of(2026, 12, 1, 15, 0, 0);

        // Act
        List<Booking> result = bookingRepository.findOverlappingBookings(1L, queryStart, queryEnd);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== H-REPO-4: Пересечение — нет (до) ====================

    /**
     * Проверяет, что при запросе периода до существующего бронирования
     * пересечений не находится.
     * Существующее 10:00–12:00, запрос 08:00–09:00.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldNotFindOverlappingBookingsWhenBefore() {
        // Arrange
        LocalDateTime existingStart = LocalDateTime.of(2026, 12, 1, 10, 0, 0);
        LocalDateTime existingEnd = LocalDateTime.of(2026, 12, 1, 12, 0, 0);

        Booking booking = Booking.builder()
                .userId(5L)
                .roomId(1L)
                .startDate(existingStart)
                .endDate(existingEnd)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        entityManager.persist(booking);
        flush();

        // Запрос 08:00–09:00 (до существующего, без пересечения)
        LocalDateTime queryStart = LocalDateTime.of(2026, 12, 1, 8, 0, 0);
        LocalDateTime queryEnd = LocalDateTime.of(2026, 12, 1, 9, 0, 0);

        // Act
        List<Booking> result = bookingRepository.findOverlappingBookings(1L, queryStart, queryEnd);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== H-REPO-5: findByName ====================

    /**
     * Проверяет поиск отеля по точному названию.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldFindByName() {
        // Arrange
        Hotel hotel = Hotel.builder()
                .name("Grand Hotel")
                .address("123 Main St")
                .build();
        entityManager.persist(hotel);
        flush();

        // Act
        Optional<Hotel> result = hotelRepository.findByName("Grand Hotel");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Grand Hotel", result.get().getName());
    }

    // ==================== H-REPO-6: findByNameContainingIgnoreCase ====================

    /**
     * Проверяет поиск отеля по подстроке в названии (без учёта регистра).
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldFindByNameContainingIgnoreCase() {
        // Arrange
        Hotel hotel = Hotel.builder()
                .name("Grand Hotel")
                .address("123 Main St")
                .build();
        entityManager.persist(hotel);
        flush();

        // Act — поиск по "grand" (нижний регистр)
        List<Hotel> result = hotelRepository.findByNameContainingIgnoreCase("grand");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Grand Hotel", result.get(0).getName());
    }

    // ==================== H-REPO-7: findByIdWithRooms ====================

    /**
     * Проверяет, что findByIdWithRooms загружает отель вместе с номерами (JOIN FETCH).
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldFindByIdWithRooms() {
        // Arrange
        Hotel hotel = Hotel.builder()
                .name("Test Hotel")
                .address("456 Test Ave")
                .build();
        entityManager.persist(hotel);

        Room room1 = Room.builder()
                .hotel(hotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();
        entityManager.persist(room1);

        Room room2 = Room.builder()
                .hotel(hotel)
                .number("102")
                .available(true)
                .timesBooked(1)
                .build();
        entityManager.persist(room2);

        flush();

        // Получаем актуальный ID отеля (сгенерирован Hibernate после flush)
        Long hotelId = entityManager.find(Hotel.class, hotel.getId()).getId();

        // Очищаем persistence context, чтобы запрос выполнился к БД заново
        entityManager.clear();

        // Act
        Optional<Hotel> found = hotelRepository.findByIdWithRooms(hotelId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Test Hotel", found.get().getName());
        assertEquals(2, found.get().getRooms().size());
    }

    // ==================== H-REPO-8: deleteTemporaryBookings ====================

    /**
     * Проверяет удаление временных бронирований (userId=0).
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldDeleteTemporaryBookings() {
        // Arrange — временное бронирование (userId=0)
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 5, 11, 0, 0);

        Booking tempBooking = Booking.builder()
                .userId(0L) // Временное бронирование
                .roomId(1L)
                .startDate(start)
                .endDate(end)
                .status(Booking.BookingStatus.PENDING)
                .build();
        entityManager.persist(tempBooking);

        // Обычное бронирование (userId=5) — должно остаться
        Booking realBooking = Booking.builder()
                .userId(5L)
                .roomId(1L)
                .startDate(start)
                .endDate(end)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        entityManager.persist(realBooking);

        flush();

        // Act
        int deletedCount = bookingRepository.deleteTemporaryBookings(1L, start, end);

        // Assert — удалено 1 временное бронирование
        assertEquals(1, deletedCount);

        // Проверяем, что реальное бронирование осталось
        List<Booking> remaining = bookingRepository.findOverlappingBookings(1L, start, end);
        assertEquals(1, remaining.size());
        assertEquals(5L, remaining.get(0).getUserId());
    }
}
