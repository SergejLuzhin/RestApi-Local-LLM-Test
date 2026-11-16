package com.skillfactory.luzhin.hotelservice.controller;

import com.skillfactory.luzhin.hotelservice.dto.HotelCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.HotelResponse;
import com.skillfactory.luzhin.hotelservice.service.HotelService;
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

import java.util.List;

/**
 * REST-контроллер для управления отелями.
 * Предоставляет эндпоинты CRUD: создание, получение списка, получение по ID, обновление, удаление.
 */
@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotel Controller", description = "API для управления отелями")
public class HotelController {

    private final HotelService hotelService;

    /**
     * Создаёт новый отель.
     * Доступно только для пользователей с ролью ADMIN.
     *
     * @param request запрос с названием и адресом отеля (обязательные поля)
     * @return DTO созданного отеля со статусом 201 Created
     */
    @PostMapping
    @Operation(
            summary = "Создать отель",
            description = "Добавляет новый отель в систему. Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "201", description = "Отель успешно создан")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelResponse> createHotel(@Valid @RequestBody HotelCreateRequest request) {
        HotelResponse response = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Возвращает список всех отелей с агрегированной информацией о номерах.
     * Доступно для авторизованных пользователей (USER и ADMIN).
     *
     * @return список DTO отелей
     */
    @GetMapping
    @Operation(
            summary = "Получить список отелей",
            description = "Возвращает все отели, зарегистрированные в системе. Доступно для USER и ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Список отелей успешно получен")
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        List<HotelResponse> hotels = hotelService.getAllHotels();
        return ResponseEntity.ok(hotels);
    }

    /**
     * Возвращает отель по идентификатору.
     * Доступно для авторизованных пользователей (USER и ADMIN).
     *
     * @param id идентификатор отеля
     * @return DTO отеля
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Получить отель по ID",
            description = "Возвращает детали отеля по уникальному идентификатору. Доступно для USER и ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Отель успешно найден")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    public ResponseEntity<HotelResponse> getHotelById(
            @Parameter(description = "Идентификатор отеля", required = true) @PathVariable Long id) {
        HotelResponse response = hotelService.getHotelById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующего отеля.
     * Доступно только для пользователей с ролью ADMIN.
     *
     * @param id идентификатор отеля для обновления
     * @param request новые название и адрес отеля
     * @return DTO обновлённого отеля
     */
    @PatchMapping("/{id}")
    @Operation(
            summary = "Обновить отель",
            description = "Обновляет название и адрес существующего отеля. Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Отель успешно обновлён")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelResponse> updateHotel(
            @Parameter(description = "Идентификатор отеля", required = true) @PathVariable Long id,
            @Valid @RequestBody HotelCreateRequest request) {
        HotelResponse response = hotelService.updateHotel(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет отель по идентификатору каскадно со всеми номерами.
     * Доступно только для пользователей с ролью ADMIN.
     *
     * @param id идентификатор отеля для удаления
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить отель",
            description = "Удаляет отель и все его номера. Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Отель успешно удалён")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHotel(
            @Parameter(description = "Идентификатор отеля", required = true) @PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
