package com.skillfactory.luzhin.bookingservice.controller;

import com.skillfactory.luzhin.bookingservice.dto.TokenResponse;
import com.skillfactory.luzhin.bookingservice.dto.UserLoginRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserRegisterRequest;
import com.skillfactory.luzhin.bookingservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для аутентификации и регистрации пользователей.
 * Предоставляет эндпоинты для регистрации новых пользователей и авторизации существующих.
 *
 * <p>Все эндпоинты этого контроллера публичные (не требуют аутентификации).
 * После регистрации или авторизации клиент получает JWT-токен,
 * который необходимо передавать в заголовке {@code Authorization: Bearer &lt;token&gt;}
 * для всех последующих запросов к защищённым ресурсам.
 *
 * <p>Срок действия JWT-токена — 1 час (конфигурируется в {@code jwt.expiration-ms}).
 *
 * @see AuthService
 * @see TokenResponse
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "API для регистрации и авторизации пользователей")
public class AuthController {

    /**
     * Сервис аутентификации и авторизации.
     * Обеспечивает регистрацию, вход и генерацию JWT-токенов.
     */
    private final AuthService authService;

    /**
     * Регистрирует нового пользователя в системе.
     *
     * <p>Процесс регистрации:
     * <ol>
     *   <li>Проверяет уникальность имени пользователя</li>
     *   <li>Хеширует пароль с помощью BCrypt</li>
     *   <li>Создаёт пользователя с ролью USER по умолчанию</li>
     *   <li>Генерирует JWT-токен со сроком действия 1 час</li>
     * </ol>
     *
     * @param request запрос на регистрацию, содержащий имя пользователя и пароль
     * @return ответ с JWT-токеном и данными зарегистрированного пользователя
     */
    @PostMapping("/register")
    @Operation(
            summary = "Зарегистрировать пользователя",
            description = "Создаёт нового пользователя с ролью USER по умолчанию. "
                    + "Имя пользователя должно быть уникальным. "
                    + "Возвращает JWT-токен для последующей аутентификации."
    )
    @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации (имя занято, короткий пароль)")
    public ResponseEntity<TokenResponse> register(
            @Parameter(description = "Данные для регистрации", required = true)
            @Valid @RequestBody UserRegisterRequest request) {

        TokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Авторизует пользователя по имени и паролю.
     *
     * <p>Процесс авторизации:
     * <ol>
     *   <li>Находит пользователя по имени</li>
     *   <li>Проверяет пароль через BCrypt</li>
     *   <li>Генерирует JWT-токен с ролью пользователя</li>
     * </ol>
     *
     * @param request запрос на авторизацию, содержащий имя пользователя и пароль
     * @return ответ с JWT-токеном и данными пользователя
     */
    @PostMapping("/auth")
    @Operation(
            summary = "Авторизовать пользователя",
            description = "Проверяет учётные данные пользователя и возвращает JWT-токен. "
                    + "Токен действителен в течение 1 часа."
    )
    @ApiResponse(responseCode = "200", description = "Авторизация успешна, токен получен")
    @ApiResponse(responseCode = "401", description = "Неверное имя пользователя или пароль")
    public ResponseEntity<TokenResponse> authenticate(
            @Parameter(description = "Учётные данные для входа", required = true)
            @Valid @RequestBody UserLoginRequest request) {

        TokenResponse response = authService.authenticate(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
