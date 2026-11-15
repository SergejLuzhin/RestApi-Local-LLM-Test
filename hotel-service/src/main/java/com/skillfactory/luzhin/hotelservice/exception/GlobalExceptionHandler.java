package com.skillfactory.luzhin.hotelservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для всех REST-контроллеров Hotel Service.
 * Преобразует исключения в структурированные ответы HTTP с кодами состояния и сообщениями об ошибках.
 *
 * <p>Особенности:
 * <ul>
 *   <li>AccessDeniedException без аутентификации → 401 (пользователь не авторизован)</li>
 *   <li>AccessDeniedException с анонимной аутентификацией → 401 (токен отсутствует или невалиден)</li>
 *   <li>AccessDeniedException с реальной аутентификацией → 403 (пользователь без нужной роли)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обрабатывает исключение ResourceNotFoundException (ресурс не найден).
     *
     * @param ex исключение
     * @return HTTP 404 Not Found с описанием ошибки
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Обрабатывает исключение RoomNotAvailableException (номер недоступен).
     *
     * @param ex исключение
     * @return HTTP 409 Conflict с описанием ошибки
     */
    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleRoomNotAvailableException(RoomNotAvailableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Обрабатывает исключение доступа (пользователь не имеет необходимой роли).
     *
     * <p>Определяет код ответа на основе статуса аутентификации:
     * <ul>
     *   <li>Если аутентификация отсутствует или является анонимной — возвращается 401 Unauthorized,
     *       так как пользователь не прошёл аутентификацию (отсутствует или невалиден JWT-токен)</li>
     *   <li>Если аутентификация реальная, но роль недостаточна — возвращается 403 Forbidden</li>
     * </ul>
     *
     * @param ex исключение
     * @return HTTP 401 Unauthorized или 403 Forbidden с описанием ошибки
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "Unauthorized");
        body.put("message", "Authentication required");

        // Проверяем статус аутентификации в текущем контексте безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Если аутентификация отсутствует или это анонимный пользователь — возвращаем 401
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // Аутентифицированный пользователь без нужной роли — возвращаем 403
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Access denied. Insufficient role.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Обрабатывает ошибку валидации входных данных (например, некорректный JSON или пропущенные обязательные поля).
     *
     * @param ex исключение
     * @return HTTP 400 Bad Request с описанием ошибок по полям
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed");

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Обрабатывает все остальные непредвиденные исключения.
     *
     * @param ex исключение
     * @return HTTP 500 Internal Server Error с описанием ошибки
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
