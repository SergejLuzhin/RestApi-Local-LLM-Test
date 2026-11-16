package com.skillfactory.luzhin.bookingservice.mapper;

import com.skillfactory.luzhin.bookingservice.dto.BookingCreateRequest;
import com.skillfactory.luzhin.bookingservice.dto.BookingResponse;
import com.skillfactory.luzhin.bookingservice.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct-маппер для преобразования между сущностью {@link Booking} и DTO.
 *
 * Обеспечивает преобразование запросов создания бронирования в сущность Booking
 * и обратно — в DTO ответа с дополнительной информацией о пользователе.
 *
 * Компонент Spring: componentModel = "spring" — создаётся как Spring-бэн для инъекции.
 */
@Mapper(componentModel = "spring")
public interface BookingMapper {

    /**
     * Преобразует запрос на создание бронирования в сущность Booking.
     *
     * @param request запрос на создание бронирования
     * @return сущность Booking с заполненными полями
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // пользователь устанавливается в сервисе
    @Mapping(target = "status", ignore = true) // статус устанавливается в сервисе
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "correlationId", ignore = true) // корреляционный ID устанавливается в сервисе
    Booking toEntity(BookingCreateRequest request);

    /**
     * Преобразует сущность Booking в DTO ответа.
     * Извлекает userId и username из связанной сущности User.
     *
     * @param booking сущность бронирования
     * @return DTO с данными бронирования и информацией о пользователе
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "roomId", source = "roomId")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    BookingResponse toResponse(Booking booking);

    /**
     * Преобразует список сущностей Booking в список DTO ответов.
     *
     * @param bookings список сущностей бронирований
     * @return список DTO ответов
     */
    List<BookingResponse> toResponseList(List<Booking> bookings);
}
