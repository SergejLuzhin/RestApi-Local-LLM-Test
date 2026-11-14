package com.skillfactory.luzhin.hotelservice.service;

import com.skillfactory.luzhin.hotelservice.dto.HotelCreateRequest;
import com.skillfactory.luzhin.hotelservice.dto.HotelResponse;
import com.skillfactory.luzhin.hotelservice.entity.Hotel;
import com.skillfactory.luzhin.hotelservice.exception.ResourceNotFoundException;
import com.skillfactory.luzhin.hotelservice.mapper.HotelMapper;
import com.skillfactory.luzhin.hotelservice.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для управления отелями.
 * Реализует CRUD-операции: создание, получение списка, получение по ID, обновление, удаление.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    /**
     * Создаёт новый отель на основе запроса.
     *
     * @param request запрос с названием и адресом отеля
     * @return DTO созданного отеля
     */
    @Transactional
    public HotelResponse createHotel(HotelCreateRequest request) {
        log.info("Creating new hotel: name={}, address={}", request.getName(), request.getAddress());

        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);

        log.info("Hotel created successfully with id={}", savedHotel.getId());
        return hotelMapper.toResponse(savedHotel);
    }

    /**
     * Возвращает список всех отелей с агрегированной информацией о номерах.
     *
     * @return список DTO отелей
     */
    @Transactional(readOnly = true)
    public List<HotelResponse> getAllHotels() {
        log.debug("Fetching all hotels");

        List<Hotel> hotels = hotelRepository.findAllWithRooms();
        log.debug("Found {} hotels", hotels.size());
        return hotelMapper.toResponseList(hotels);
    }

    /**
     * Возвращает отель по идентификатору.
     *
     * @param id идентификатор отеля
     * @return DTO отеля
     * @throws ResourceNotFoundException если отель не найден
     */
    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long id) {
        log.debug("Fetching hotel by id={}", id);

        Hotel hotel = hotelRepository.findByIdWithRooms(id)
                .orElseThrow(() -> {
                    log.warn("Hotel not found with id={}", id);
                    return new ResourceNotFoundException("Hotel", id);
                });

        return hotelMapper.toResponse(hotel);
    }

    /**
     * Обновляет данные существующего отеля.
     *
     * @param id идентификатор отеля для обновления
     * @param request новые название и адрес
     * @return DTO обновлённого отеля
     * @throws ResourceNotFoundException если отель не найден
     */
    @Transactional
    public HotelResponse updateHotel(Long id, HotelCreateRequest request) {
        log.info("Updating hotel id={} with name={}, address={}", id, request.getName(), request.getAddress());

        Hotel hotel = hotelRepository.findByIdWithRooms(id)
                .orElseThrow(() -> {
                    log.warn("Hotel not found for update with id={}", id);
                    return new ResourceNotFoundException("Hotel", id);
                });

        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());

        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated successfully with id={}", updatedHotel.getId());
        return hotelMapper.toResponse(updatedHotel);
    }

    /**
     * Удаляет отель по идентификатору каскадно со всеми номерами.
     *
     * @param id идентификатор отеля для удаления
     * @throws ResourceNotFoundException если отель не найден
     */
    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting hotel id={}", id);

        Hotel hotel = hotelRepository.findByIdWithRooms(id)
                .orElseThrow(() -> {
                    log.warn("Hotel not found for deletion with id={}", id);
                    return new ResourceNotFoundException("Hotel", id);
                });

        hotelRepository.delete(hotel);
        log.info("Hotel deleted successfully with id={}", id);
    }
}
