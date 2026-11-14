package com.skillfactory.luzhin.hotelservice.mapper;

import com.skillfactory.luzhin.hotelservice.dto.RoomCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.RoomResponse;
import com.skillfactory.luzhin.hotelservice.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct-маппер для преобразования между сущностью Room и DTO RoomCreateRequest / RoomResponse.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RoomMapper {

    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    /**
     * Преобразует запрос создания номера в сущность Room.
     * Ссылка на отель устанавливается отдельно после загрузки отеля из БД.
     *
     * @param request запрос с идентификатором отеля, номером комнаты и доступностью
     * @return сущность Room с заполненными полями
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", ignore = true)
    Room toEntity(RoomCreateRequest request);

    /**
     * Преобразует сущность Room в DTO ответа RoomResponse.
     * Извлекает информацию об отеле (id и name) через связь ManyToOne.
     *
     * @param room сущность номера
     * @return DTO с полной информацией о номере
     */
    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomResponse toResponse(Room room);

    /**
     * Преобразует список сущностей Room в список DTO RoomResponse.
     *
     * @param rooms список сущностей номеров
     * @return список DTO номеров
     */
    List<RoomResponse> toResponseList(List<Room> rooms);
}
