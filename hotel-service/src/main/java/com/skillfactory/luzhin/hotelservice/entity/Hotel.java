package com.skillfactory.luzhin.hotelservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность «Отель». Представляет гостиницу с адресом и набором номеров.
 * Связана с номерами через отношение OneToMany с каскадным удалением.
 */
@Entity
@Table(name = "hotels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Hotel extends BaseEntity {

    /**
     * Уникальный идентификатор отеля.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название отеля.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Адрес отеля.
     */
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    /**
     * Список номеров отеля. Каскадное удаление при удалении отеля.
     * Связь инициализируется лениво.
     */
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    /**
     * Добавляет номер в список номеров отеля.
     * Автоматически устанавливает ссылку на отель в номере.
     *
     * @param room номер для добавления
     */
    public void addRoom(Room room) {
        rooms.add(room);
        room.setHotel(this);
    }

    /**
     * Удаляет номер из списка номеров отеля.
     * Автоматически сбрасывает ссылку на отель в номере.
     *
     * @param room номер для удаления
     */
    public void removeRoom(Room room) {
        rooms.remove(room);
        room.setHotel(null);
    }
}
