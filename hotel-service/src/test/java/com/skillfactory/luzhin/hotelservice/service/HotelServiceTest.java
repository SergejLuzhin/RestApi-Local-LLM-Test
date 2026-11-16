package com.skillfactory.luzhin.hotelservice.service;

import com.skillfactory.luzhin.hotelservice.dto.HotelCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.HotelResponse;
import com.skillfactory.luzhin.hotelservice.entity.Hotel;
import com.skillfactory.luzhin.hotelservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.hotelservice.mapper.HotelMapper;
import com.skillfactory.luzhin.hotelservice.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для сервиса управления отелями.
 * Проверяет бизнес-логику CRUD-операций без загрузки Spring-контекста.
 *
 * <p>Тесты охватывают все операции сервиса:
 * <ul>
 *   <li>Создание отеля</li>
 *   <li>Получение списка всех отелей</li>
 *   <li>Обновление отеля (успешное и при отсутствии)</li>
 *   <li>Удаление отеля (успешное и при отсутствии)</li>
 * </ul>
 *
 * @see HotelService
 * @see HotelRepository
 * @see HotelMapper
 */
@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    /**
     * Моки для репозитория и маппера.
     */
    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    /**
     * Сервис, тестируемый в данном тестовом классе.
     * Инъекция моков через конструктор.
     */
    @InjectMocks
    private HotelService hotelService;

    // ==================== H-HS-1: Создание отеля ====================

    /**
     * Проверяет успешное создание отеля.
     * HotelMapper.toEntity() преобразует запрос в сущность,
     * HotelRepository.save() сохраняет её, HotelMapper.toResponse() возвращает DTO.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldCreateHotelSuccessfully() {
        // Arrange
        HotelCreateRequest request = HotelCreateRequest.builder()
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .build();

        Hotel savedHotel = Hotel.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .build();

        HotelResponse expectedResponse = HotelResponse.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .totalRooms(0)
                .availableRooms(0)
                .build();

        when(hotelMapper.toEntity(request)).thenReturn(savedHotel);
        when(hotelRepository.save(any(Hotel.class))).thenReturn(savedHotel);
        when(hotelMapper.toResponse(savedHotel)).thenReturn(expectedResponse);

        // Act
        HotelResponse result = hotelService.createHotel(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Grand Hotel", result.getName());
        assertEquals("123 Main St, New York", result.getAddress());

        // Verify — каждый маппинг и сохранение вызваны ровно 1 раз
        verify(hotelMapper, times(1)).toEntity(request);
        verify(hotelRepository, times(1)).save(any(Hotel.class));
        verify(hotelMapper, times(1)).toResponse(savedHotel);
    }

    // ==================== H-HS-2: Получение всех отелей ====================

    /**
     * Проверяет получение списка всех отелей с агрегацией информации о номерах.
     * HotelRepository.findAllWithRooms() загружает отели с JOIN FETCH номеров.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldGetAllHotels() {
        // Arrange
        Hotel hotel1 = Hotel.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .build();

        Hotel hotel2 = Hotel.builder()
                .id(2L)
                .name("Small Inn")
                .address("456 Oak Ave, Boston")
                .build();

        HotelResponse response1 = HotelResponse.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, New York")
                .totalRooms(10)
                .availableRooms(8)
                .build();

        HotelResponse response2 = HotelResponse.builder()
                .id(2L)
                .name("Small Inn")
                .address("456 Oak Ave, Boston")
                .totalRooms(5)
                .availableRooms(3)
                .build();

        when(hotelRepository.findAllWithRooms()).thenReturn(List.of(hotel1, hotel2));
        when(hotelMapper.toResponseList(List.of(hotel1, hotel2))).thenReturn(List.of(response1, response2));

        // Act
        List<HotelResponse> result = hotelService.getAllHotels();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Grand Hotel", result.get(0).getName());
        assertEquals("Small Inn", result.get(1).getName());

        // Verify
        verify(hotelRepository, times(1)).findAllWithRooms();
        verify(hotelMapper, times(1)).toResponseList(List.of(hotel1, hotel2));
    }

    // ==================== H-HS-3: Обновление отеля ====================

    /**
     * Проверяет успешное обновление отеля.
     * Отель найден, поля name и address обновлены, save() вызван.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldUpdateHotelSuccessfully() {
        // Arrange
        Long hotelId = 1L;
        HotelCreateRequest request = HotelCreateRequest.builder()
                .name("Updated Hotel Name")
                .address("789 New Address, Chicago")
                .build();

        Hotel existingHotel = Hotel.builder()
                .id(hotelId)
                .name("Old Hotel Name")
                .address("100 Old Address")
                .build();

        Hotel updatedHotel = Hotel.builder()
                .id(hotelId)
                .name("Updated Hotel Name")
                .address("789 New Address, Chicago")
                .build();

        HotelResponse expectedResponse = HotelResponse.builder()
                .id(hotelId)
                .name("Updated Hotel Name")
                .address("789 New Address, Chicago")
                .totalRooms(0)
                .availableRooms(0)
                .build();

        when(hotelRepository.findByIdWithRooms(hotelId)).thenReturn(Optional.of(existingHotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(updatedHotel);
        when(hotelMapper.toResponse(updatedHotel)).thenReturn(expectedResponse);

        // Act
        HotelResponse result = hotelService.updateHotel(hotelId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Hotel Name", result.getName());
        assertEquals("789 New Address, Chicago", result.getAddress());

        // Verify
        verify(hotelRepository, times(1)).findByIdWithRooms(hotelId);
        verify(hotelRepository, times(1)).save(any(Hotel.class));
        verify(hotelMapper, times(1)).toResponse(updatedHotel);
    }

    // ==================== H-HS-4: Обновление несуществующего отеля ====================

    /**
     * Проверяет, что при попытке обновления несуществующего отеля
     * выбрасывается ResourceNotFoundException.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldThrowExceptionWhenUpdatingMissingHotel() {
        // Arrange
        Long hotelId = 999L;
        HotelCreateRequest request = HotelCreateRequest.builder()
                .name("Non-existent Hotel")
                .address("Nowhere Street")
                .build();

        when(hotelRepository.findByIdWithRooms(hotelId)).thenReturn(Optional.empty());

        // Act & Assert — должно выбросить ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> hotelService.updateHotel(hotelId, request)
        );

        assertTrue(exception.getMessage().contains("Hotel"));
        assertTrue(exception.getMessage().contains("999"));

        // Verify — save не вызывался (отель не найден, обновление невозможно)
        verify(hotelRepository, times(1)).findByIdWithRooms(hotelId);
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    // ==================== H-HS-5: Удаление отеля ====================

    /**
     * Проверяет успешное удаление отеля.
     * Отель найден, delete() вызван.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldDeleteHotelSuccessfully() {
        // Arrange
        Long hotelId = 1L;
        Hotel hotel = Hotel.builder()
                .id(hotelId)
                .name("Hotel to Delete")
                .address("100 Delete Street")
                .build();

        when(hotelRepository.findByIdWithRooms(hotelId)).thenReturn(Optional.of(hotel));

        // Act — не должно выбросить исключение
        assertDoesNotThrow(() -> hotelService.deleteHotel(hotelId));

        // Verify — отель найден и удалён
        verify(hotelRepository, times(1)).findByIdWithRooms(hotelId);
        verify(hotelRepository, times(1)).delete(hotel);
    }

    // ==================== H-HS-6: Удаление несуществующего отеля ====================

    /**
     * Проверяет, что при попытке удаления несуществующего отеля
     * выбрасывается ResourceNotFoundException.
     *
     * @throws Exception ошибка при выполнении теста
     */
    @Test
    void shouldThrowExceptionWhenDeletingMissingHotel() {
        // Arrange
        Long hotelId = 999L;

        when(hotelRepository.findByIdWithRooms(hotelId)).thenReturn(Optional.empty());

        // Act & Assert — должно выбросить ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> hotelService.deleteHotel(hotelId)
        );

        assertTrue(exception.getMessage().contains("Hotel"));
        assertTrue(exception.getMessage().contains("999"));

        // Verify — delete не вызывался (отель не найден)
        verify(hotelRepository, times(1)).findByIdWithRooms(hotelId);
        verify(hotelRepository, never()).delete(any(Hotel.class));
    }
}
