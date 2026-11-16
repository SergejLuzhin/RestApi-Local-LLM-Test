package com.skillfactory.luzhin.bookingservice.service;

import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.entity.Booking;
import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import com.skillfactory.luzhin.bookingservice.entity.User;
import com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.bookingservice.mapper.BookingMapper;
import com.skillfactory.luzhin.bookingservice.repository.BookingRepository;
import com.skillfactory.luzhin.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис управления бронированиями.
 *
 * <p>Обеспечивает создание, получение и отмену бронирований номеров в отелях.
 * На данном этапе реализован скелет функциональности — бронирования создаются
 * в статусе PENDING без вызова Hotel Service для подтверждения доступности.
 *
 * <p>Полноценный двухшаговый процесс бронирования (PENDING → CONFIRMED/CANCELLED
 * с вызовом Hotel Service через Feign) будет реализован в后续 этапах.
 *
 * <p>Основная функциональность:
 * <ul>
 *   <li>{@code createBooking} — создание бронирования в статусе PENDING</li>
 *   <li>{@code getBookingsByUserId} — получение истории бронирований пользователя</li>
 *   <li>{@code getBookingById} — получение бронирования по идентификатору</li>
 *   <li>{@code cancelBooking} — отмена бронирования (перевод в статус CANCELLED)</li>
 * </ul>
 *
 * <p>Корреляционный идентификатор (correlationId) генерируется как UUID
 * для каждого бронирования и используется для сквозного логирования процесса.
 *
 * @see BookingStatus
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingService {

    /**
     * Репозиторий для операций с сущностью Booking.
     */
    private final BookingRepository bookingRepository;

    /**
     * Репозиторий для операций с сущностью User.
     */
    private final UserRepository userRepository;

    /**
     * Маппер для преобразования между сущностью Booking и DTO.
     */
    private final BookingMapper bookingMapper;

    /**
     * Создаёт новое бронирование в статусе PENDING.
     *
     * <p>Процесс создания (текущий этап — скелет):
     * <ol>
     *   <li>Генерирует уникальный корреляционный идентификатор (correlationId)</li>
     *   <li>Находит пользователя по идентификатору</li>
     *   <li>Создаёт бронирование в статусе PENDING</li>
     *   <li>Сохраняет бронирование в базе данных</li>
     * </ol>
     *
     * <p>В后续 этапах здесь будет вызов Hotel Service для подтверждения доступности номера
     * с двухшаговой согласованностью (PENDING → CONFIRMED / PENDING → CANCELLED).
     *
     * @param request запрос на создание бронирования
     * @param userId  идентификатор пользователя, создающего бронирование
     * @return DTO с данными созданного бронирования
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, Long userId) {
        // Генерируем корреляционный идентификатор для сквозного логирования
        String correlationId = UUID.randomUUID().toString();
        log.info("Начало процесса создания бронирования. correlationId={}, userId={}, roomId={}",
                correlationId, userId, request.getRoomId());

        // Находим пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        // Создаём бронирование в статусе PENDING
        Booking booking = bookingMapper.toEntity(request);
        booking.setUser(user);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCorrelationId(correlationId);

        // Сохраняем бронирование
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Бронирование создано в статусе PENDING. correlationId={}, bookingId={}",
                correlationId, savedBooking.getId());

        return bookingMapper.toResponse(savedBooking);
    }

    /**
     * Получает все бронирования указанного пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список DTO с данными бронирований пользователя
     */
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookingMapper.toResponseList(bookings);
    }

    /**
     * Получает бронирование по уникальному идентификатору.
     *
     * <p>Проверяет, что бронирование принадлежит указанному пользователю
     * (для обеспечения безопасности — пользователь видит только свои бронирования).
     *
     * @param bookingId идентификатор бронирования
     * @param userId    идентификатор пользователя (для проверки владения)
     * @return DTO с данными бронирования
     * @throws ResourceNotFoundException если бронирование не найдено или не принадлежит пользователю
     */
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdWithUser(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование", bookingId));

        // Проверяем, что бронирование принадлежит пользователю
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Бронирование", bookingId);
        }

        return bookingMapper.toResponse(booking);
    }

    /**
     * Отменяет бронирование, переводя его в статус CANCELLED.
     *
     * <p>Отмена возможна только для бронирований в статусе PENDING или CONFIRMED.
     * Бронирование в статусе CANCELLESS нельзя отменить повторно.
     *
     * @param bookingId идентификатор бронирования
     * @param userId    идентификатор пользователя (для проверки владения)
     * @throws ResourceNotFoundException если бронирование не найдено или не принадлежит пользователю
     */
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdWithUser(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование", bookingId));

        // Проверяем, что бронирование принадлежит пользователю
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Бронирование", bookingId);
        }

        // Проверяем, что бронирование ещё не отменено
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Бронирование уже отменено");
        }

        // Переводим в статус CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Бронирование отменено. bookingId={}, correlationId={}",
                bookingId, booking.getCorrelationId());
    }
}
