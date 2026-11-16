package com.skillfactory.luzhin.bookingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillfactory.luzhin.bookingservice.exception.HotelServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Декодер ошибок для Feign-клиента Hotel Service.
 *
 * <p>Преобразует HTTP-ответы с кодами ошибок (4xx, 5xx) в исключения Java.
 * Извлекает сообщения об ошибках из тела HTTP-ответа (в формате JSON)
 * и формирует информативные сообщения для дальнейшей обработки.
 *
 * <p>Поддерживаемые коды ответов:
 * <ul>
 *   <li><b>409 Conflict</b> — номер недоступен на запрошенные даты (RoomNotAvailableException)</li>
 *   <li><b>404 Not Found</b> — ресурс не найден</li>
 *   <li><b>5xx</b> — внутренние ошибки сервера</li>
 *   <li><b>4xx</b> — другие ошибки клиента</li>
 * </ul>
 *
 * @see HotelServiceException
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    /**
     * Декодер JSON для извлечения сообщений из тела HTTP-ответа.
     */
    private final ObjectMapper objectMapper;

    /**
     * Базовый декодер ошибок Feign (стандартная реализация).
     */
    private final ErrorDecoder defaultErrorDecoder;

    /**
     * Создаёт новый декодер ошибок.
     *
     * @param objectMapper mapper для десериализации JSON из тела HTTP-ответа
     * @param defaultErrorDecoder стандартный декодер ошибок Feign для fallback-обработки
     */
    public FeignErrorDecoder(ObjectMapper objectMapper, ErrorDecoder defaultErrorDecoder) {
        this.objectMapper = objectMapper;
        this.defaultErrorDecoder = defaultErrorDecoder;
    }

    /**
     * Декодирует HTTP-ответ с ошибкой в исключение Java.
     *
     * <p>Логика обработки:
     * <ol>
     *   <li>Извлекает тело HTTP-ответа (если есть)</li>
     *   <li>Пытается десериализовать JSON в Map для извлечения поля message</li>
     *   <li>Формирует информативное сообщение об ошибке</li>
     *   <li>Выбрасывает {@code HotelServiceException} с соответствующим сообщением</li>
     * </ol>
     *
     * @param sMethod ключ метода Feign (например, "confirmAvailability")
     * @param response HTTP-ответ с ошибкой
     * @return исключение, которое будет выброшено в вызывающий код
     */
    @Override
    public Exception decode(String sMethod, Response response) {
        String errorMessage = extractErrorMessage(response);
        HttpStatus status = HttpStatus.valueOf(response.status());

        log.error(
                "Ошибка вызова Hotel Service. method={}, status={}, message={}, url={}",
                sMethod, status.value(), errorMessage, response.request().url()
        );

        // Формируем исключение с информацией о статусе и методе
        String detailedMessage = String.format(
                "Ошибка Hotel Service [%s]: %s. Метод: %s",
                status.getReasonPhrase(), errorMessage, sMethod
        );

        return new HotelServiceException(detailedMessage);
    }

    /**
     * Извлекает сообщение об ошибке из тела HTTP-ответа.
     *
     * <p>Пытается десериализовать JSON-тело ответа в Map и извлечь поле "message".
     * Если десериализация не удалась, возвращает тело ответа как строку.
     * Если тело пустое, возвращает стандартное сообщение по коду статуса.
     *
     * @param response HTTP-ответ
     * @return извлечённое сообщение об ошибке
     */
    private String extractErrorMessage(Response response) {
        try {
            // Пытаемся прочитать тело ответа через InputStream
            java.io.InputStream inputStream = response.body().asInputStream();
            byte[] bodyBytes = inputStream.readAllBytes();
            inputStream.close();
            String body = bodyBytes != null && bodyBytes.length > 0
                    ? new String(bodyBytes, StandardCharsets.UTF_8) : null;

            if (body != null && !body.isEmpty()) {
                // Пытаемся распарсить JSON и извлечь message
                Map<String, Object> errorBody = objectMapper.readValue(
                        body, Map.class
                );

                // Проверяем наличие поля message
                if (errorBody.containsKey("message")) {
                    return errorBody.get("message").toString();
                }

                // Если message нет, возвращаем всё тело
                return body;
            }
        } catch (Exception e) {
            log.warn("Не удалось извлечь сообщение об ошибке из HTTP-ответа. status={}",
                    response.status(), e);
        }

        // Fallback: стандартное сообщение по коду статуса
        return HttpStatus.valueOf(response.status()).getReasonPhrase();
    }
}
