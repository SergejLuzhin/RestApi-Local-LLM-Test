package com.skillfactory.luzhin.bookingservice.client;

import com.skillfactory.luzhin.bookingservice.client.dto.AvailabilityResponse;
import com.skillfactory.luzhin.bookingservice.client.dto.ReleaseResponse;
import com.skillfactory.luzhin.bookingservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * Feign-клиент для взаимодействия с Hotel Management Service.
 *
 * <p>Обеспечивает удалённый вызов внутренних эндпоинтов Hotel Service
 * для подтверждения доступности номеров и снятия временных блокировок
 * в рамках двухшаговой транзакции бронирования.
 *
 * <p>Основные методы:
 * <ul>
 *   <li>{@code confirmAvailability} — подтверждение доступности номера на заданный период
 *       с временной блокировкой слота;</li>
 *   <li>{@code releaseSlot} — компенсация: снятие временной блокировки слота.</li>
 * </ul>
 *
 * <p>Имя клиента соответствует serviceId в Eureka Server — {@code hotel-service}.
 * Конфигурация тайм-аутов, JWT-токена и обработки ошибок определяется классом
 * {@link com.skillfactory.luzhin.bookingservice.config.FeignConfig}.
 *
 * @see com.skillfactory.luzhin.bookingservice.config.FeignConfig
 */
@FeignClient(
        name = "hotel-service",
        url = "${hotel.service.url:http://localhost:8081}",
        configuration = FeignConfig.class
)
public interface HotelServiceClient {

    /**
     * Подтверждает доступность номера на запрошенные даты.
     *
     * <p>Выполняет проверку пересечения заданных дат с существующими бронированиями.
     * При отсутствии пересечений создаёт временную блокировку слота.
     * Вызывается из {@code BookingService.createBooking} на втором шаге двухшаговой транзакции.
     *
     * @param roomId        идентификатор номера для проверки
     * @param start         дата и время начала бронирования
     * @param end           дата и время окончания бронирования
     * @param correlationId сквозной идентификатор корреляции (для логирования)
     * @return DTO с результатом проверки доступности
     * @throws com.skillfactory.luzhin.bookingservice.exception.HotelServiceException
     *         если номер недоступен или возникла ошибка удалённого сервиса
     */
    @PostMapping("/api/rooms/{roomId}/confirm-availability")
    AvailabilityResponse confirmAvailability(
            @PathVariable("roomId") Long roomId,
            @RequestParam("start") LocalDateTime start,
            @RequestParam("end") LocalDateTime end,
            @RequestParam("correlationId") String correlationId
    );

    /**
     * Компенсирующее действие: снимает временную блокировку слота номера.
     *
     * <p>Удаляет временное бронирование (с userId=0), созданное при подтверждении доступности.
     * Вызывается из {@code BookingService} при сбое подтверждения бронирования
     * для отмены ранее созданной блокировки.
     *
     * @param roomId        идентификатор номера, для которого снимается блокировка
     * @param start         дата и время начала бронирования
     * @param end           дата и время окончания бронирования
     * @param correlationId сквозной идентификатор корреляции (для логирования)
     * @return DTO с результатом операции снятия блокировки
     * @throws com.skillfactory.luzhin.bookingservice.exception.HotelServiceException
     *         если возникла ошибка при снятии блокировки
     */
    @PostMapping("/api/rooms/{roomId}/release")
    ReleaseResponse releaseSlot(
            @PathVariable("roomId") Long roomId,
            @RequestParam("start") LocalDateTime start,
            @RequestParam("end") LocalDateTime end,
            @RequestParam("correlationId") String correlationId
    );
}
