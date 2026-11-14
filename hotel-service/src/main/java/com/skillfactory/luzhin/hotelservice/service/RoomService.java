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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для управления номерами отеля.
 * Реализует CRUD-операции для номеров, а также методы для подтверждения доступности
 * и снятия временной блокировки (compensation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    /**
     * Создаёт новый номер в указанном отеле.
     *
     * @param request запрос с идентификатором отеля, номером комнаты и доступностью
     * @return DTO созданного номера
     * @throws ResourceNotFoundException если отель с указанным ID не найден
     */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {
        log.info("Creating new room: hotelId={}, number={}, available={}",
                request.getHotelId(), request.getNumber(), request.getAvailable());

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> {
                    log.warn("Hotel not found for room creation with id={}", request.getHotelId());
                    return new ResourceNotFoundException("Hotel", request.getHotelId());
                });

        Room room = roomMapper.toEntity(request);
        room.setHotel(hotel);
        room.setTimesBooked(0);

        // Сохраняем номер напрямую через RoomRepository для получения сгенерированного ID
        Room savedRoom = roomRepository.save(room);
        log.info("Room created successfully with id={}, hotelId={}", savedRoom.getId(), request.getHotelId());
        return roomMapper.toResponse(savedRoom);
    }

    /**
     * Возвращает все номере, которые операционно доступны (available = true).
     * Не учитывает занятость по датам — только операционное состояние.
     *
     * @return список доступных номеров
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllAvailableRooms() {
        log.debug("Fetching all available rooms");

        List<Room> rooms = roomRepository.findByAvailableTrue();
        log.debug("Found {} available rooms", rooms.size());
        return roomMapper.toResponseList(rooms);
    }

    /**
     * Возвращает доступные номера, отсортированные по алгоритму равномерной загрузки.
     * Сортировка: по возрастанию timesBooked, при равенстве — по возрастанию id.
     * Используется Booking Service при автоподборе номера (autoSelect=true).
     *
     * @return список рекомендуемых номеров
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getRecommendedRooms() {
        log.debug("Fetching recommended rooms (sorted by timesBooked ASC, id ASC)");

        List<Room> rooms = roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc();
        log.debug("Found {} recommended rooms", rooms.size());
        return roomMapper.toResponseList(rooms);
    }

    /**
     * Возвращает номер по идентификатору.
     *
     * @param id идентификатор номера
     * @return DTO номера
     * @throws ResourceNotFoundException если номер не найден
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        log.debug("Fetching room by id={}", id);

        Room room = roomRepository.findByIdWithHotel(id)
                .orElseThrow(() -> {
                    log.warn("Room not found with id={}", id);
                    return new ResourceNotFoundException("Room", id);
                });

        return roomMapper.toResponse(room);
    }

    /**
     * Подтверждает доступность номера на запрошенные даты.
     * Проверяет пересечение заданных дат с существующими бронированиями (PENDING и CONFIRMED).
     * Если пересечение обнаружено — выбрасывает RoomNotAvailableException.
     * При отсутствии пересечений — регистрирует временную блокировку слота путём создания
     * локального бронирования в Hotel Service (для отслеживания занятости).
     *
     * @param roomId      идентификатор номера
     * @param start       дата и время начала бронирования
     * @param end         дата и время окончания бронирования
     * @param correlationId сквозной идентификатор корреляции (для трассировки распределённой транзакции)
     * @throws RoomNotAvailableException если номер уже забронирован на заданный период
     */
    @Transactional
    public void confirmAvailability(Long roomId, LocalDateTime start, LocalDateTime end, String correlationId) {
        log.info("Confirming availability for room={}, start={}, end={}, correlationId={}",
                roomId, start, end, correlationId);

        // 1. Проверяем существование номера
        Room room = roomRepository.findByIdWithHotel(roomId)
                .orElseThrow(() -> {
                    log.warn("Room not found for availability check with id={}", roomId);
                    return new ResourceNotFoundException("Room", roomId);
                });

        // 2. Проверяем пересечение с существующими бронированиями (PENDING, CONFIRMED)
        // В Hotel Service бронирования создаются локально при confirmAvailability.
        // При двухшаговой транзакции: если Booking Service не завершил создание,
        // то здесь будет только временное бронирование (userId=0).
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(roomId, start, end);
        if (!overlappingBookings.isEmpty()) {
            log.warn("Room {} is not available: overlapping bookings found, correlationId={}", roomId, correlationId);
            throw new RoomNotAvailableException(roomId, "Room is already booked for the specified dates");
        }

        // 3. Создаём временную блокировку (локальное бронирование в Hotel Service)
        // Это позволяет отслеживать занятость номера в рамках Hotel Service
        Booking tempBooking = Booking.builder()
                .userId(0L) // Временный userId, реальное значение хранится в Booking Service
                .roomId(roomId)
                .startDate(start)
                .endDate(end)
                .status(Booking.BookingStatus.PENDING)
                .build();

        bookingRepository.save(tempBooking);
        log.info("Availability confirmed for room={}, correlationId={}, temporary slot blocked",
                roomId, correlationId);
    }

    /**
     * Компенсирующее действие: снимает временную блокировку слота номера.
     * Вызывается при ошибке подтверждения бронирования (сбой Hotel Service, таймаут и т.д.).
     * Удаляет временное бронирование (PENDING), созданное при confirmAvailability.
     *
     * @param roomId      идентификатор номера
     * @param start       дата и время начала бронирования
     * @param end         дата и время окончания бронирования
     * @param correlationId сквозной идентификатор корреляции
     */
    @Transactional
    public void releaseSlot(Long roomId, LocalDateTime start, LocalDateTime end, String correlationId) {
        log.info("Releasing slot for room={}, start={}, end={}, correlationId={}",
                roomId, start, end, correlationId);

        // Удаляем все временные бронирования (userId=0) для этого номера в заданном периоде
        int deletedCount = bookingRepository.deleteTemporaryBookings(roomId, start, end);

        log.info("Slot released for room={}, correlationId={}, temporary bookings deleted={}",
                roomId, correlationId, deletedCount);
    }
}
