package com.skillfactory.luzhin.hotelservice.mapper;

import com.skillfactory.luzhin.hotelservice.dto.HotelCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.HotelResponse;
import com.skillfactory.luzhin.hotelservice.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct-маппер для преобразования между сущностью Hotel и DTO HotelCreateRequest / HotelResponse.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface HotelMapper {

    /**
     * Преобразует запрос создания отеля в сущность Hotel.
     *
     * @param request запрос с названием и адресом отеля
     * @return сущность Hotel с заполненными полями
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    Hotel toEntity(HotelCreateRequest request);

    /**
     * Преобразует сущность Hotel в DTO ответа HotelResponse.
     * Вычисляет общее количество номеров и количество доступных номеров.
     *
     * @param hotel сущность отеля
     * @return DTO с полной информацией об отеле
     */
    @Mapping(target = "totalRooms", expression = "java(hotel.getRooms() != null ? hotel.getRooms().size() : 0)")
    @Mapping(target = "availableRooms", expression = "java(hotel.getRooms() != null ? (int) hotel.getRooms().stream().filter(r -> r.getAvailable() != null && r.getAvailable()).count() : 0)")
    HotelResponse toResponse(Hotel hotel);

    /**
     * Преобразует список сущностей Hotel в список DTO HotelResponse.
     *
     * @param hotels список сущностей отелей
     * @return список DTO отелей
     */
    List<HotelResponse> toResponseList(List<Hotel> hotels);
}
