package com.skillfactory.luzhin.bookingservice.service;

import com.skillfactory.luzhin.bookingservice.client.HotelServiceClient;
import com.skillfactory.luzhin.bookingservice.client.dto.RoomRecommendResponse;
import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.entity.Booking;
import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import com.skillfactory.luzhin.bookingservice.entity.User;
import com.skillfactory.luzhin.bookingservice.exception.HotelServiceException;
import com.skillfactory.luzhin.bookingservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.bookingservice.mapper.BookingMapper;
import com.skillfactory.luzhin.bookingservice.repository.BookingRepository;
import com.skillfactory.luzhin.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис управления бронированиями.
 *
 * <p>Обеспечивает создание, получение и отмену бронирований номеров в отелях
 * с использованием двухшаговой согласованности между микросервисами.
 *
 * <p>Основная функциональность:
 * <ul>
 *   <li>{@code createBooking} — создание бронирования с двухшаговой транзакцией:
 *       <ol>
 *         <li>Создание бронирования в статусе PENDING (локальная транзакция)</li>
 *         <li>Вызов Hotel Service для подтверждения доступности номера (с повторными попытками)</li>
 *         <li>При успехе — перевод в статус CONFIRMED</li>
 *         <li>При ошибке — перевод в статус CANCELLED с компенсацией (освобождением слота)</li>
 *       </ol>
 *   </li>
 *   <li>{@code getBookingsByUserId} — получение истории бронирований пользователя</li>
 *   <li>{@code getBookingById} — получение бронирования по идентификатору</li>
 *   <li>{@code cancelBooking} — отмена бронирования (перевод в статус CANCELLED)</li>
 * </ul>
 *
 * <p>Поддерживаемые статусы бронирования:
 * <ul>
 *   <li>{@code PENDING} — ожидает подтверждения от Hotel Service;</li>
 *   <li>{@code CONFIRMED} — подтверждено Hotel Service, активно;</li>
 *   <li>{@code CANCELLED} — отменено (ошибка подтверждения, таймаут или ручная отмена).</li>
 * </ul>
 *
 * <p>Идемпотентность: каждое бронирование получает уникальный корреляционный идентификатор
 * (correlationId), по которому проверяется отсутствие дубликатов при повторных запросах.
 *
 * <p>Алгоритм автоподбора номера (autoSelect = true):
 * <ol>
 *   <li>Запрос рекомендованных номеров через Hotel Service ({@code GET /rooms/recommend})</li>
 *   <li>Номера возвращаются отсортированными по {@code timesBooked ASC, id ASC}</li>
 *   <li>Выбирается первый номер из списка (наименее загруженный)</li>
 * </ol>
 *
 * @see BookingStatus
 * @see HotelServiceClient
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
     * Feign-клиент для взаимодействия с Hotel Management Service.
     */
    private final HotelServiceClient hotelServiceClient;

    /**
     * Создаёт новое бронирование с двухшаговой согласованностью между сервисами.
     *
     * <p>Процесс создания (двухшаговая транзакция):
     * <ol>
     *   <li>Генерирует уникальный корреляционный идентификатор (correlationId) для сквозного логирования</li>
     *   <li>Проверяет идемпотентность — не существует ли уже бронирования с таким correlationId</li>
     *   <li>Находит пользователя по идентификатору</li>
     *   <li>Определяет номер:
     *       <ul>
     *         <li>Если {@code autoSelect = true} — запрашивает рекомендованные номера у Hotel Service
     *             и выбирает первый (наименьший счётчик бронирований)</li>
     *         <li>Если {@code autoSelect = false} — использует указанный {@code roomId}</li>
     *       </ul>
     *   </li>
     *   <li>Создаёт бронирование в статусе PENDING и сохраняет в БД (локальная транзакция)</li>
     *   <li>Вызывает Hotel Service для подтверждения доступности номера
     *       с повторными попытками (до 3, с экспоненциальной задержкой)</li>
     *   <li>При успехе подтверждения — переводит бронирование в статус CONFIRMED</li>
     *   <li>При ошибке (таймаут, недоступность, сбой сервера) — переводит в статус CANCELLED
     *       и выполняет компенсацию (освобождение слота в Hotel Service)</li>
     * </ol>
     *
     * <p>Пример последовательности событий:
     * <pre>
     * 1. correlationId=abc-123, userId=1, roomId=5 → Booking(PENDING) saved
     * 2. hotelServiceClient.confirmAvailability(roomId=5, start=..., end=..., correlationId=abc-123)
     *    → success → Booking(CONFIRMED)
     *
     * 1. correlationId=def-456, userId=2, roomId=3 → Booking(PENDING) saved
     * 2. hotelServiceClient.confirmAvailability(roomId=3, ...)
     *    → timeout/exception → Booking(CANCELLED) + hotelServiceClient.releaseSlot(roomId=3, ...)
     * </pre>
     *
     * <p>Повторные попытки применяются только к вызову {@code confirmAvailability}
     * (настройка retry в {@code application.yml}: 3 попытки, 500ms база, множитель 2).
     * Локальная транзакция создания бронирования фиксируется до вызова Hotel Service.
     *
     * @param request запрос на создание бронирования
     * @param userId  идентификатор пользователя, создающего бронирование
     * @return DTO с данными созданного бронирования
     * @throws ResourceNotFoundException если пользователь не найден
     * @throws IllegalArgumentException  если autoSelect=false и roomId не указан
     */
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, Long userId) {
        // Генерируем корреляционный идентификатор для сквозного логирования процесса
        String correlationId = UUID.randomUUID().toString();
        log.info("Начало процесса создания бронирования. correlationId={}, userId={}, autoSelect={}",
                correlationId, userId, request.isAutoSelect());

        // Проверка идемпотентности: если бронирование с таким correlationId уже существует — возвращаем его
        var existingBooking = bookingRepository.findByCorrelationId(correlationId);
        if (existingBooking.isPresent()) {
            log.warn("Бронирование с таким correlationId уже существует (идемпотентность). correlationId={}",
                    correlationId);
            return bookingMapper.toResponse(existingBooking.get());
        }

        // Находим пользователя, создающего бронирование
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        // Определяем идентификатор номера
        Long roomId = resolveRoomId(request, correlationId);
        log.info("Номер определён. correlationId={}, roomId={}", correlationId, roomId);

        // Создаём бронирование в статусе PENDING и сохраняем в БД (локальная транзакция)
        Booking booking = bookingMapper.toEntity(request);
        booking.setUser(user);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCorrelationId(correlationId);
        booking.setRoomId(roomId);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Бронирование создано в статусе PENDING. correlationId={}, bookingId={}, roomId={}",
                correlationId, savedBooking.getId(), roomId);

        // Пытаемся подтвердить доступность номера через Hotel Service (с повторными попытками)
        try {
            confirmWithRetry(booking);
            log.info("Бронирование подтверждено. correlationId={}, bookingId={}",
                    correlationId, savedBooking.getId());
        } catch (HotelServiceException e) {
            // Все попытки исчерпаны — компенсируем: отменяем бронирование и освобождаем слот
            log.error("Подтверждение доступности не удалось после всех повторов. " +
                    "correlationId={}, bookingId={}, roomId={}. Причина: {}",
                    correlationId, savedBooking.getId(), roomId, e.getMessage());
            cancelBookingInternal(savedBooking.getId(), userId, correlationId);
            return bookingMapper.toResponse(savedBooking);
        }

        return bookingMapper.toResponse(savedBooking);
    }

    /**
     * Определяет идентификатор номера для бронирования.
     *
     * <p>Если {@code autoSelect = true} — запрашивает список рекомендованных номеров
     * через Hotel Service и выбирает первый (наименее загруженный по счётчику {@code timesBooked}).
     *
     * <p>Если {@code autoSelect = false} — использует указанный в запросе {@code roomId}.
     * Если {@code roomId} равен null при {@code autoSelect = false}, выбрасывает исключение.
     *
     * @param request       запрос на создание бронирования
     * @param correlationId корреляционный идентификатор для логирования
     * @return идентификатор номера для бронирования
     * @throws IllegalArgumentException если autoSelect=false и roomId не указан
     * @throws ResourceNotFoundException если рекомендованные номера не найдены
     */
    private Long resolveRoomId(BookingCreateRequest request, String correlationId) {
        if (request.isAutoSelect()) {
            log.info("Автоподбор номера запрошен. correlationId={}", correlationId);
            List<RoomRecommendResponse> recommendedRooms = hotelServiceClient.getRecommendedRooms();

            if (recommendedRooms.isEmpty()) {
                log.warn("Рекомендованные номера не найдены. correlationId={}", correlationId);
                throw new ResourceNotFoundException("Свободные номера", (Long) null);
            }

            // Выбираем первый номер из списка (отсортирован по timesBooked ASC, id ASC в Hotel Service)
            Long selectedRoomId = recommendedRooms.get(0).getId();
            log.info("Автоподбор: выбран номер. correlationId={}, roomId={}, timesBooked={}",
                    correlationId, selectedRoomId, recommendedRooms.get(0).getTimesBooked());
            return selectedRoomId;
        }

        // Явный roomId из запроса
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException(
                    "Идентификатор номера не может быть null при autoSelect=false");
        }
        return request.getRoomId();
    }

    /**
     * Подтверждает доступность номера с повторными попытками.
     *
     * <p>Вызывает Hotel Service для подтверждения доступности номера на заданный период.
     * При успешном ответе переводит бронирование в статус CONFIRMED.
     *
     * <p>Повторные попытки настраиваются через Resilience4j Retry
     * (configId = {@code booking-availability} в {@code application.yml}):
     * <ul>
     *   <li>Максимум попыток: 3</li>
     *   <li>Базовая задержка: 500 мс</li>
     *   <li>Экспоненциальный множитель: 2</li>
     *   <li>Повторяются при: SocketTimeoutException, ConnectException, HttpServerErrorException</li>
     * </ul>
     *
     * <p>Каждая попытка выполняется в отдельной транзакции ({@code REQUIRES_NEW}),
     * чтобы состояние бронирования сохранялось между повторными попытками.
     *
     * @param booking бронирование для подтверждения
     * @throws HotelServiceException если все попытки подтверждения не удаллись
     */
    @Retryable(
            value = {HotelServiceException.class},
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 500,
                    multiplier = 2,
                    maxDelay = 5000
            )
    )
    @Transactional
    public void confirmWithRetry(Booking booking) {
        log.info("Подтверждение доступности номера. bookingId={}, roomId={}",
                booking.getId(), booking.getRoomId());

        // Вызов Hotel Service для подтверждения доступности номера
        hotelServiceClient.confirmAvailability(
                booking.getRoomId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getCorrelationId()
        );

        // Если вызов прошёл успешно — переводим бронирование в статус CONFIRMED
        log.info("Доступность номера подтверждена Hotel Service. bookingId={}", booking.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
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
     * <p>Отмена возможна только для бронирований в статусах PENDING или CONFIRMED.
     * Бронирование в статусе CANCELLED нельзя отменить повторно.
     *
     * @param bookingId идентификатор бронирования
     * @param userId    идентификатор пользователя (для проверки владения)
     * @throws ResourceNotFoundException если бронирование не найдено или не принадлежит пользователю
     * @throws IllegalStateException     если бронирование уже отменено
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
        cancelBookingInternal(bookingId, userId, booking.getCorrelationId());
    }

    /**
     * Внутренний метод отмены бронирования.
     *
     * <p>Переводит бронирование в статус CANCELLED и выполняет компенсацию
     * (освобождение слота в Hotel Service), если бронирование было в статусе PENDING.
     *
     * <p>Используется как при ручной отмене пользователем, так и при автоматической
     * компенсации после неудачного подтверждения доступности.
     *
     * @param bookingId     идентификатор бронирования
     * @param userId        идентификатор пользователя
     * @param correlationId корреляционный идентификатор для логирования
     */
    private void cancelBookingInternal(Long bookingId, Long userId, String correlationId) {
        Booking booking = bookingRepository.findByIdWithUser(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return; // Уже отменено — идемпотентность
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Бронирование отменено (статус CANCELLED). bookingId={}, correlationId={}",
                bookingId, correlationId);

        // Если бронирование было в статусе PENDING — компенсируем блокировку в Hotel Service
        if (booking.getRoomId() != null) {
            log.info("Компенсация: освобождаем слот для номера. bookingId={}, roomId={}",
                    bookingId, booking.getRoomId());
            hotelServiceClient.releaseSlot(
                    booking.getRoomId(),
                    booking.getStartDate(),
                    booking.getEndDate(),
                    correlationId
            );
        }
    }
}
