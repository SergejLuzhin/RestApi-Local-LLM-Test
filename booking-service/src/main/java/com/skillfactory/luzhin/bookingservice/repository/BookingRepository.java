package com.skillfactory.luzhin.bookingservice.repository;

import com.skillfactory.luzhin.bookingservice.entity.Booking;
import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для выполнения CRUD-операций с сущностью {@link Booking}.
 *
 * Предоставляет методы для поиска бронирований по пользователю, статусу,
 * а также проверки пересечений дат для заданного номера.
 *
 * Методы:
 * - findByUserId — поиск всех бронирований конкретного пользователя;
 * - findByStatus — поиск бронирований по статусу;
 * - existsByRoomIdAndOverlapDates — проверка наличия активных бронирований с пересечением дат;
 * - findByIdWithUser — поиск бронирования по ID с загруженным пользователем;
 * - findByCorrelationId — поиск бронирования по идентификатору корреляции.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Находит все бронирования указанного пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список бронирований пользователя
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Находит все бронирования с указанным статусом.
     *
     * @param status статус бронирования для поиска
     * @return список бронирований с указанным статусом
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Проверяет наличие активных бронирований (PENDING или CONFIRMED) для указанного номера,
     * даты которых пересекаются с указанным периодом.
     *
     * Пересечение дат: новое бронирование пересекается, если его начало раньше окончания
     * существующего и его конец позже начала существующего.
     *
     * @param roomId  идентификатор номера
     * @param start   дата начала проверяемого периода
     * @param end     дата окончания проверяемого периода
     * @return true, если найдено хотя бы одно пересекающееся активное бронирование; false — в противном случае
     */
    @Query("SELECT EXISTS (" +
           "SELECT 1 FROM Booking b " +
           "WHERE b.roomId = :roomId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND b.startDate < :end " +
           "AND b.endDate > :start" +
           ")")
    boolean existsByRoomIdAndOverlapDates(
            @Param("roomId") Long roomId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Находит бронирование по уникальному идентификатору корреляции.
     *
     * Используется для проверки идемпотентности при повторных запросах.
     *
     * @param correlationId идентификатор корреляции
     * @return Optional с найденным бронированием, пустой — если не найдено
     */
    Optional<Booking> findByCorrelationId(String correlationId);

    /**
     * Находит бронирование по ID с загруженной информацией о пользователе.
     * Использует JOIN FETCH для предотвращения проблемы N+1 запросов.
     *
     * @param id идентификатор бронирования
     * @return Optional с найденным бронированием и пользователем, пустой — если не найдено
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.id = :id")
    Optional<Booking> findByIdWithUser(@Param("id") Long id);
}
