package com.skillfactory.luzhin.hotelservice.controller;

import com.skillfactory.luzhin.hotelservice.dto.RoomCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.RoomResponse;
import com.skillfactory.luzhin.hotelservice.service.RoomService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST-контроллер для управления номерами отеля.
 * Предоставляет эндпоинты CRUD для номеров, а также методы подтверждения доступности
 * и снятия временной блокировки (для двухшаговой транзакции с Booking Service).
 */
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Controller", description = "API для управления номерами отелей")
public class RoomController {

    private final RoomService roomService;

    /**
     * Создаёт новый номер в указанном отеле.
     * Доступно только для пользователей с ролью ADMIN.
     *
     * @param request запрос с идентификатором отеля, номером комнаты и доступностью
     * @return DTO созданного номера со статусом 201 Created
     */
    @PostMapping
    @Operation(
            summary = "Создать номер",
            description = "Добавляет новый номер в указанный отель. Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "201", description = "Номер успешно создан")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Возвращает все номере, которые операционно доступны.
     * Доступно для авторизованных пользователей (USER и ADMIN).
     *
     * @return список доступных номеров
     */
    @GetMapping
    @Operation(
            summary = "Получить доступные номера",
            description = "Возвращает все номера, которые операционно доступны. Не учитывает занятость по датам. Доступно для USER и ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Список номеров успешно получен")
    public ResponseEntity<List<RoomResponse>> getAllAvailableRooms() {
        List<RoomResponse> rooms = roomService.getAllAvailableRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * Возвращает рекомендуемые номера, отсортированные по алгоритму равномерной загрузки.
     * Доступно для авторизованных пользователей (USER и ADMIN).
     *
     * @return список рекомендуемых номеров (сортировка по timesBooked ASC, id ASC)
     */
    @GetMapping("/recommend")
    @Operation(
            summary = "Получить рекомендуемые номера",
            description = "Возвращает доступные номера, отсортированные по возрастанию количества бронирований. "
                    + "Используется Booking Service для автоподбора номера. Доступно для USER и ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Список рекомендованных номеров успешно получен")
    public ResponseEntity<List<RoomResponse>> getRecommendedRooms() {
        List<RoomResponse> rooms = roomService.getRecommendedRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * Подтверждает доступность номера на запрошенные даты.
     * Выполняет временную блокировку слота для поддержки двухшаговой транзакции.
     * Внутренний эндпоинт (не публикуется через Gateway), вызывается из Booking Service.
     *
     * @param roomId идентификатор номера
     * @param start  дата и время начала бронирования
     * @param end    дата и время окончания бронирования
     * @param correlationId сквозной идентификатор корреляции
     */
    @PostMapping("/{roomId}/confirm-availability")
    @Hidden
    @Operation(
            summary = "Подтвердить доступность номера",
            description = "Внутренний эндпоинт для подтверждения доступности номера на заданный период. "
                    + "Создаёт временную блокировку. Вызывается из Booking Service."
    )
    @ApiResponse(responseCode = "200", description = "Доступность подтверждена, слот заблокирован")
    @ApiResponse(responseCode = "409", description = "Номер недоступен на заданный период")
    public ResponseEntity<Void> confirmAvailability(
            @Parameter(description = "Идентификатор номера", required = true) @PathVariable Long roomId,
            @Parameter(description = "Дата и время начала", required = true) @RequestParam LocalDateTime start,
            @Parameter(description = "Дата и время окончания", required = true) @RequestParam LocalDateTime end,
            @Parameter(description = "Корреляционный идентификатор", required = true) @RequestParam String correlationId) {
        roomService.confirmAvailability(roomId, start, end, correlationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Компенсирующее действие: снимает временную блокировку слота номера.
     * Внутренний эндпоинт (не публикуется через Gateway), вызывается из Booking Service при сбое.
     *
     * @param roomId идентификатор номера
     * @param start  дата и время начала бронирования
     * @param end    дата и время окончания бронирования
     * @param correlationId сквозной идентификатор корреляции
     */
    @PostMapping("/{roomId}/release")
    @Hidden
    @Operation(
            summary = "Снять временную блокировку",
            description = "Внутренний эндпоинт для компенсации: снимает временную блокировку слота. "
                    + "Вызывается из Booking Service при ошибке подтверждения бронирования."
    )
    @ApiResponse(responseCode = "200", description = "Блокировка успешно снята")
    public ResponseEntity<Void> releaseSlot(
            @Parameter(description = "Идентификатор номера", required = true) @PathVariable Long roomId,
            @Parameter(description = "Дата и время начала", required = true) @RequestParam LocalDateTime start,
            @Parameter(description = "Дата и время окончания", required = true) @RequestParam LocalDateTime end,
            @Parameter(description = "Корреляционный идентификатор", required = true) @RequestParam String correlationId) {
        roomService.releaseSlot(roomId, start, end, correlationId);
        return ResponseEntity.ok().build();
    }
}
