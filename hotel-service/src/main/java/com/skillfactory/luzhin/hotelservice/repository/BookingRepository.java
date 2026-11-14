package com.skillfactory.luzhin.hotelservice.repository;

import com.skillfactory.luzhin.hotelservice.entity.Booking;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для выполнения операций с локальными бронированиями в Hotel Service.
 * Используется для проверки пересечений дат и управления временными блокировками.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Находит все бронирования для указанного номера, пересекающиеся с заданным периодом.
     * Учитываются только бронирования в статусах PENDING и CONFIRMED.
     *
     * @param roomId идентификатор номера
     * @param start  дата начала проверяемого периода
     * @param end    дата конца проверяемого периода
     * @return список пересекающихся бронирований
     */
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status IN ('PENDING', 'CONFIRMED') "
            + "AND b.startDate < :end AND b.endDate > :start")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /**
     * Находит все временные бронирования для номера, пересекающиеся с заданным периодом.
     * Временные бронирования имеют userId = 0 (созданы при confirmAvailability).
     *
     * @param roomId идентификатор номера
     * @param start  дата начала проверяемого периода
     * @param end    дата конца проверяемого периода
     * @return список временных бронирований
     */
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.userId = 0 AND b.status IN ('PENDING', 'CONFIRMED') "
            + "AND b.startDate < :end AND b.endDate > :start")
    List<Booking> findTemporaryBookings(@Param("roomId") Long roomId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    /**
     * Удаляет все временные бронирования для номера, пересекающиеся с заданным периодом.
     *
     * @param roomId идентификатор номера
     * @param start  дата начала проверяемого периода
     * @param end    дата конца проверяемого периода
     * @return количество удалённых бронирований
     */
    @Modifying
    @Query("DELETE FROM Booking b WHERE b.roomId = :roomId AND b.userId = 0 AND b.status IN ('PENDING', 'CONFIRMED') "
            + "AND b.startDate < :end AND b.endDate > :start")
    int deleteTemporaryBookings(@Param("roomId") Long roomId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
