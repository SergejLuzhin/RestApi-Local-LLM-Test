package com.skillfactory.luzhin.bookingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Конфигурация Feign-клиента для Hotel Service.
 *
 * <p>Настройки:
 * <ul>
 *   <li><b>Тайм-ауты</b>: подключение — 5 секунд, чтение — 10 секунд;</li>
 *   <li><b>JWT-токен</b>: автоматически добавляется в заголовок {@code Authorization}
 *       для всех запросов к Hotel Service;</li>
 *   <li><b>Обработка ошибок</b>: кастомный {@link FeignErrorDecoder} для маппинга
 *       HTTP-ошибок в {@link com.skillfactory.luzhin.bookingservice.exception.HotelServiceException}.</li>
 * </ul>
 *
 * <p>Данный конфигурационный класс применяется через {@code @FeignClient(configuration = FeignConfig.class)}
 * к Feign-клиентам, требующим взаимодействия с Hotel Service.
 */
@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    /**
     * Запрос HTTP, из которого извлекается JWT-токен для передачи в Hotel Service.
     *
     * @return текущий HTTP-запрос из контекста
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * Создаёт интерсептор запросов для автоматического добавления JWT-токена.
     *
     * <p>Интерсептор извлекает заголовок {@code Authorization} из текущего
     * HTTP-запроса и добавляет его во все запросы, отправляемые через Feign-клиент
     * к Hotel Service. Это обеспечивает сквозную аутентификацию между микросервисами.
     *
     * <p>Если токен отсутствует в исходном запросе, заголовок не добавляется
     * (для внутренних вызовов, не требующих аутентификации).
     *
     * @return интерсептор {@code RequestInterceptor}
     */
    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                HttpServletRequest request = getCurrentRequest();
                if (request != null) {
                    String authorizationHeader = request.getHeader("Authorization");
                    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                        template.header("Authorization", authorizationHeader);
                    }
                }
            }
        };
    }

    /**
     * Создаёт декодер ошибок для Feign-клиента.
     *
     * <p>Кастомный декодер преобразует HTTP-ответы с кодами 4xx и 5xx
     * в исключение {@code HotelServiceException} с информативным сообщением.
     * Это позволяет обрабатывать ошибки удалённого вызова в бизнес-логике
     * (например, при создании бронирования — перевод в статус CANCELLED).
     *
     * @param objectMapper mapper для десериализации JSON из тела HTTP-ответа
     * @return кастомный декодер ошибок
     */
    @Bean
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new FeignErrorDecoder(objectMapper, new ErrorDecoder.Default());
    }

    /**
     * Создаёт конфигурацию тайм-аутов для HTTP-запросов Feign-клиента.
     *
     * <p>Параметры тайм-аутов:
     * <ul>
     *   <li><b>Connect timeout</b>: 5000 мс (5 секунд) — время ожидания установления TCP-соединения;</li>
     *   <li><b>Read timeout</b>: 10000 мс (10 секунд) — время ожидания получения ответа от сервера.</li>
     * </ul>
     *
     * <p>Эти значения обеспечивают баланс между быстрой реакцией на недоступные сервисы
     * и достаточным временем для обработки запросов в условиях сетевых задержек.
     * При превышении тайм-аута Feign выбрасывает {@code ReadTimeoutException},
     * который перехватывается {@link FeignErrorDecoder} и преобразуется
     * в {@link com.skillfactory.luzhin.bookingservice.exception.HotelServiceException}.
     *
     * @return конфигурация тайм-аутов {@code Request.Options}
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000,  // connect timeout — 5 секунд на установление соединения
                10000  // read timeout — 10 секунд на получение ответа
        );
    }
}
