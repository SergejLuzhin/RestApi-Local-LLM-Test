package com.skillfactory.luzhin.bookingservice.controller;

import com.skillfactory.luzhin.bookingservice.dto.UserRegisterRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserResponse;
import com.skillfactory.luzhin.bookingservice.dto.UserUpdateRequest;
import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import com.skillfactory.luzhin.bookingservice.service.UserService;
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
 * REST-контроллер для административного управления пользователями.
 * Предоставляет CRUD-операции для пользователей с ролью ADMIN.
 *
 * <p>Все эндпоинты этого контроллера защищены и доступны только пользователям
 * с ролью ADMIN. Обычные пользователи (USER) не могут создавать, обновлять
 * или удалять пользователей через этот контроллер.
 *
 * <p>Использование:
 * <ul>
 *   <li>POST /user — создать пользователя (ADMIN)</li>
 *   <li>PATCH /user — обновить данные пользователя (ADMIN)</li>
 *   <li>DELETE /user — удалить пользователя (ADMIN)</li>
 *   <li>GET /users — получить список всех пользователей (ADMIN)</li>
 *   <li>GET /users/{id} — получить пользователя по ID (ADMIN)</li>
 * </ul>
 *
 * @see UserService
 * @see UserRole
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Controller", description = "API для административного управления пользователями")
public class UserController {

    /**
     * Сервис управления пользователями.
     * Обеспечивает CRUD-операции с пользователями.
     */
    private final UserService userService;

    /**
     * Создаёт нового пользователя с указанными данными.
     * Роль пользователя устанавливается на USER по умолчанию.
     *
     * @param request запрос на создание пользователя, содержащий имя и пароль
     * @return DTO созданного пользователя со статусом 201 Created
     */
    @PostMapping
    @Operation(
            summary = "Создать пользователя",
            description = "Создаёт нового пользователя с ролью USER по умолчанию. "
                    + "Требует роль ADMIN. Имя пользователя должно быть уникальным."
    )
    @ApiResponse(responseCode = "201", description = "Пользователь успешно создан")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации (имя занято, короткий пароль)")
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true)
            @Valid @RequestBody UserRegisterRequest request) {

        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Обновляет данные существующего пользователя.
     *
     * <p>Обновляются только переданные поля (username и/или password).
     * Если поле не передано (null), его значение не изменяется.
     * Пароль автоматически хешируется перед сохранением.
     *
     * @param id      идентификатор пользователя для обновления
     * @param request запрос с обновлёнными данными (username и/или password)
     * @return DTO с обновлёнными данными пользователя
     */
    @PatchMapping
    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет имя пользователя и/или пароль. "
                    + "Все поля опциональны — обновляются только переданные значения. "
                    + "Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Идентификатор пользователя", required = true)
            @RequestParam Long id,
            @Parameter(description = "Обновлённые данные пользователя", required = true)
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет пользователя по идентификатору.
     *
     * <p>Удаление пользователя выполняется каскадно — все бронирования
     * этого пользователя также удаляются (CascadeType.ALL).
     *
     * @param id идентификатор пользователя для удаления
     */
    @DeleteMapping
    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя и все его бронирования из системы. "
                    + "Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Пользователь успешно удалён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Идентификатор пользователя", required = true)
            @RequestParam Long id) {

        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Получает список всех пользователей системы.
     *
     * @return список DTO с данными всех пользователей
     */
    @GetMapping("/users")
    @Operation(
            summary = "Получить список пользователей",
            description = "Возвращает список всех пользователей системы без паролей. "
                    + "Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Получает пользователя по уникальному идентификатору.
     *
     * @param id идентификатор пользователя
     * @return DTO с данными пользователя
     */
    @GetMapping("/users/{id}")
    @Operation(
            summary = "Получить пользователя по ID",
            description = "Возвращает данные пользователя по уникальному идентификатору. "
                    + "Требует роль ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Пользователь успешно найден")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "Идентификатор пользователя", required = true)
            @PathVariable Long id) {

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}
