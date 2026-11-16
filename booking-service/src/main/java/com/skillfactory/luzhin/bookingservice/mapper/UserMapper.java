package com.skillfactory.luzhin.bookingservice.mapper;

import com.skillfactory.luzhin.bookingservice.dto.UserRegisterRequest;
import com.skillfactory.luzhin.bookingservice.dto.UserResponse;
import com.skillfactory.luzhin.bookingservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct-маппер для преобразования между сущностью {@link User} и DTO.
 *
 * Обеспечивает преобразование запросов регистрации/обновления в сущность User
 * и обратно — в DTO ответа.
 *
 * Компонент Spring: componentModel = "spring" — создаётся как Spring-бэн для инъекции.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Преобразует запрос регистрации пользователя в сущность User.
     *
     * @param request запрос на регистрацию
     * @return сущность User с заполненными полями
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // пароль хешируется в сервисе
    @Mapping(target = "role", ignore = true)      // роль задаётся в сервисе
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRegisterRequest request);

    /**
     * Преобразует сущность User в DTO ответа.
     *
     * @param user сущность пользователя
     * @return DTO с данными пользователя (без пароля)
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserResponse toResponse(User user);

    /**
     * Преобразует список сущностей User в список DTO ответов.
     *
     * @param users список сущностей пользователей
     * @return список DTO ответов
     */
    List<UserResponse> toResponseList(List<User> users);
}
