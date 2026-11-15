package com.skillfactory.luzhin.hotelservice.repository;

import com.skillfactory.luzhin.hotelservice.entity.Booking;
import com.skillfactory.luzhin.hotelservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для выполнения CRUD-операций с сущностью Room.
 * Предоставляет кастомные методы для поиска доступных номеров,
 * сортировки по загруженности и проверки пересечений бронирований.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Находит все номера, которые операционно доступны (available = true).
     *
     * @return список доступных номеров
     */
    List<Room> findByAvailableTrue();

    /**
     * Находит все доступные номера, отсортированные по возрастанию количества бронирований,
     * а при равенстве — по возрастанию идентификатора.
     * Используется алгоритмом автоподбора комнат для равномерной загрузки.
     *
     * @return список доступных номеров, отсортированных по timesBooked ASC, id ASC
     */
    List<Room> findByAvailableTrueOrderByTimesBookedAscIdAsc();

    /**
     * Находит все номера отеля, которые операционно доступны.
     *
     * @param hotelId идентификатор отеля
     * @return список доступных номеров отеля
     */
    List<Room> findByHotelIdAndAvailableTrue(Long hotelId);

    /**
     * Проверяет наличие пересечений бронирований для номера на заданный период.
     * Пересечение дат: новое бронирование пересекается с существующим, если его даты
     * перекрываются с датами существующего бронирования в статусе PENDING или CONFIRMED.
     *
     * @param roomId идентификатор номера
     * @param start дата начала проверяемого периода
     * @param end   дата конца проверяемого периода
     * @return список бронирований, пересекающихся с заданным периодом
     */
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status IN ('PENDING', 'CONFIRMED') "
            + "AND b.startDate < :end AND b.endDate > :start")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /**
     * Находит номер по идентификатору вместе с информацией об отеле.
     *
     * @param id идентификатор номера
     * @return Optional с найденным номером и отелем
     */
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.hotel WHERE r.id = :id")
    Optional<Room> findByIdWithHotel(@Param("id") Long id);
}
