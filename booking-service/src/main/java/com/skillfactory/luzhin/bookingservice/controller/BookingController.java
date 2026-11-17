package com.skillfactory.luzhin.bookingservice.controller;

import com.skillfactory.luzhin.bookingservice.config.JwtUtil;
import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления бронированиями.
 * Предоставляет эндпоинты для создания, просмотра и отмены бронирований.
 *
 * <p>Все эндпоинты этого контроллера защищены и доступны только пользователям
 * с ролью USER. Администраторы (ADMIN) также могут использовать эти эндпоинты,
 * так как {@code hasRole('USER')} допускает обе роли в данной конфигурации.
 *
 * <p>Основные операции:
 * <ul>
 *   <li>POST /bookings — создание бронирования (с выбором или автоподбором номера)</li>
 *   <li>GET /bookings/{id} — получение бронирования по идентификатору</li>
 *   <li>GET /bookings — получение истории бронирований текущего пользователя</li>
 *   <li>DELETE /bookings/{id} — отмена бронирования</li>
 * </ul>
 *
 * <p>При создании бронирования поддерживается два режима:
 * <ul>
 *   <li>{@code autoSelect = false} — пользователь указывает конкретный номер через {@code roomId}</li>
 *   <li>{@code autoSelect = true} — система автоматически выбирает наименее загруженный номер</li>
 * </ul>
 *
 * @see BookingService
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "Booking Controller", description = "API для управления бронированиями")
public class BookingController {

    /**
     * Сервис управления бронированиями.
     * Обеспечивает создание, получение и отмену бронирований
     * с двухшаговой согласованностью между сервисами.
     */
    private final BookingService bookingService;

    /**
     * Утилита для работы с JWT-токенами.
     * Используется для извлечения userId из токена.
     */
    private final JwtUtil jwtUtil;

    /**
     * Извлекает JWT-токен из заголовка Authorization HTTP-запроса.
     *
     * @param request HTTP-запрос
     * @return токен без префикса "Bearer "
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Создаёт новое бронирование с двухшаговой согласованностью.
     *
     * <p>Процесс создания (двухшаговая транзакция):
     * <ol>
     *   <li>Создаёт бронирование в статусе PENDING (локальная транзакция)</li>
     *   <li>Вызывает Hotel Service для подтверждения доступности номера</li>
     *   <li>При успехе — переводит в статус CONFIRMED</li>
     *   <li>При ошибке — переводит в статус CANCELLED с компенсацией (освобождением слота)</li>
     * </ol>
     *
     * <p>Если {@code autoSelect = true}, номер подбирается автоматически
     * на основе алгоритма равномерной загрузки (наименьшее значение {@code timesBooked}).
     *
     * @param request запрос на создание бронирования
     * @param httpRequest HTTP-запрос для извлечения JWT-токена
     * @return DTO созданного бронирования
     */
    @PostMapping
    @Operation(
            summary = "Создать бронирование",
            description = "Создаёт бронирование номера с двухшаговой согласованностью: "
                    + "сначала создаётся в статусе PENDING, затем подтверждается Hotel Service. "
                    + "Поддерживается автоподбор номера (autoSelect = true). "
                    + "Требует роль USER."
    )
    @ApiResponse(responseCode = "200", description = "Бронирование успешно создано или подтверждено")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации или данных запроса")
    @ApiResponse(responseCode = "502", description = "Ошибка Hotel Service (таймаут, недоступность)")
    public ResponseEntity<BookingResponse> createBooking(
            @Parameter(description = "Данные для создания бронирования", required = true)
            @Valid @RequestBody BookingCreateRequest request,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        Long userId = jwtUtil.getUserId(token);
        BookingResponse response = bookingService.createBooking(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает бронирование по уникальному идентификатору.
     *
     * <p>Проверяет, что бронирование принадлежит запрашивающему пользователю.
     * Пользователь может просматривать только свои бронирования.
     *
     * @param id идентификатор бронирования
     * @param httpRequest HTTP-запрос для извлечения JWT-токена
     * @return DTO с данными бронирования
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Получить бронирование по ID",
            description = "Возвращает данные бронирования по уникальному идентификатору. "
                    + "Пользователь видит только свои бронирования. Требует роль USER."
    )
    @ApiResponse(responseCode = "200", description = "Бронирование успешно найдено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    public ResponseEntity<BookingResponse> getBookingById(
            @Parameter(description = "Идентификатор бронирования", required = true)
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        Long userId = jwtUtil.getUserId(token);
        BookingResponse response = bookingService.getBookingById(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получает историю бронирований текущего пользователя.
     *
     * <p>Возвращает все бронирования текущего авторизованного пользователя
     * в порядке создания (от новых к старым или наоборот, в зависимости от порядка репозитория).
     *
     * @param httpRequest HTTP-запрос для извлечения JWT-токена
     * @return список DTO с данными бронирований пользователя
     */
    @GetMapping("/bookings")
    @Operation(
            summary = "Получить историю бронирований",
            description = "Возвращает все бронирования текущего пользователя. "
                    + "Требует роль USER."
    )
    @ApiResponse(responseCode = "200", description = "История бронирований успешно получена")
    public ResponseEntity<List<BookingResponse>> getBookings(
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        Long userId = jwtUtil.getUserId(token);
        List<BookingResponse> bookings = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Отменяет бронирование, переводя его в статус CANCELLED.
     *
     * <p>Отмена возможна только для бронирований в статусах PENDING или CONFIRMED.
     * Бронирование в статусе CANCELLEMENT нельзя отменить повторно.
     *
     * @param id идентификатор бронирования
     * @param httpRequest HTTP-запрос для извлечения JWT-токена
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Отменить бронирование",
            description = "Отменяет бронирование (переводит в статус CANCELLED). "
                    + "Возможно только для бронирований в статусах PENDING или CONFIRMED. "
                    + "Требует роль USER."
    )
    @ApiResponse(responseCode = "204", description = "Бронирование успешно отменено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "400", description = "Бронирование уже отменено")
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "Идентификатор бронирования", required = true)
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        Long userId = jwtUtil.getUserId(token);
        bookingService.cancelBooking(id, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
