package com.skillfactory.luzhin.hotelservice.repository;

import com.skillfactory.luzhin.hotelservice.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для выполнения CRUD-операций с сущностью Hotel.
 * Предоставляет методы поиска отелей по имени и получения всех отелей.
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    /**
     * Находит отель по точному названию.
     *
     * @param name название отеля
     * @return Optional с найденным отелем, пустой если не найден
     */
    Optional<Hotel> findByName(String name);

    /**
     * Находит все отели, содержащие указанную подстроку в названии (без учёта регистра).
     *
     * @param name подстрока для поиска
     * @return список найденных отелей
     */
    List<Hotel> findByNameContainingIgnoreCase(String name);

    /**
     * Находит все отели вместе с их номерами (использует JOIN FETCH для предотвращения N+1).
     *
     * @return список всех отелей с загруженными номерами
     */
    @Query("SELECT DISTINCT h FROM Hotel h LEFT JOIN FETCH h.rooms")
    List<Hotel> findAllWithRooms();

    /**
     * Находит отель по ID вместе с номерами.
     *
     * @param id идентификатор отеля
     * @return Optional с найденным отелем и номерами
     */
    @Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.rooms WHERE h.id = :id")
    Optional<Hotel> findByIdWithRooms(@Param("id") Long id);
}
