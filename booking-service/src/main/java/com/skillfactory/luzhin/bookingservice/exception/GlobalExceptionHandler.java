package com.skillfactory.luzhin.bookingservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для всех REST-контроллеров Booking Service.
 * Перехватывает исключения и возвращает стандартизированные JSON-ответы с информацией об ошибке.
 *
 * <p>Обрабатываемые исключения:
 * <ul>
 *   <li>{@link ResourceNotFoundException} — ресурс не найден → HTTP 404</li>
 *   <li>{@link AccessDeniedException} — недостаточных прав доступа → HTTP 401 (если не авторизован) / HTTP 403</li>
 *   <li>{@link MethodArgumentNotValidException} — ошибка валидации DTO → HTTP 400 с детализацией по полям</li>
 *   <li>{@link ConstraintViolationException} — нарушение ограничений → HTTP 400</li>
 *   <li>{@link Exception} — неизвестная ошибка → HTTP 500</li>
 * </ul>
 *
 * @see org.springframework.web.bind.annotation.RestControllerAdvice
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * DTO для стандартизированного JSON-ответа с информацией об ошибке.
     */
    private record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        /**
         * Создаёт ответ об ошибке без детализации по полям.
         */
        ErrorResponse(Instant timestamp, int status, String error, String message) {
            this(timestamp, status, error, message, null);
        }
    }

    /**
     * Обработчик исключения «Ресурс не найден».
     *
     * @param exception исключение с описанием найденного и отсутствующего ресурса
     * @return ResponseEntity с HTTP 404 и JSON-описанием ошибки
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Обработчик исключения недостаточных прав доступа.
     * Возвращает HTTP 401, если пользователь не аутентифицирован (anonymous),
     * или HTTP 403, если пользователь аутентифицирован, но не имеет нужной роли.
     *
     * @param exception исключение недостаточных прав доступа
     * @return ResponseEntity с HTTP 401 или 403 и JSON-описанием ошибки
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Обработчик ошибки валидации DTO-параметров.
     * Возвращает HTTP 400 с детализацией ошибок по каждому полю.
     *
     * @param exception исключение валидации аргумента метода
     * @return ResponseEntity с HTTP 400 и JSON с полем fieldErrors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обработчик нарушения ограничений (например, @Size, @Min).
     *
     * @param exception исключение нарушения ограничений
     * @return ResponseEntity с HTTP 400 и JSON-описанием ошибки
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обработчик неизвестных исключений.
     *
     * @param exception любое необработанное исключение
     * @return ResponseEntity с HTTP 500 и JSON-описанием ошибки
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Internal server error: " + exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
