# План тестирования системы бронирования отелей

## Структура

| Сервис | Этап реализации тестов | Статус |
|--------|----------------------|--------|
| Hotel Service | Этап 7 | ⏳ В процессе |
| Booking Service | Этап 16 | ⏳ Отложено |
| API Gateway | Этап 17 | ⏳ Отложено |
| Eureka Server | — | ✅ Не требуется |

---

## 1. Hotel Service

> **Этап реализации:** Этап 7

### 1.1. `HotelControllerTest` — `@WebMvcTest + @AutoConfigureMockMvc`

| # | Тест | Метод | Путь | Что проверяет | Ожидаемый результат |
|---|------|-------|------|---------------|---------------------|
| H-CT-1 | Создание отеля | `POST` | `/api/hotels` | Валидные данные, токен ADMIN | `201 Created`, тело ответа содержит `id` |
| H-CT-2 | Валидация создания отеля | `POST` | `/api/hotels` | Пустое тело (невалидные данные) | `400 Bad Request`, ошибки валидации по полям `name` и `address` |
| H-CT-3 | Создание отеля без токена | `POST` | `/api/hotels` | Отсутствие JWT-токена | `401 Unauthorized` |
| H-CT-4 | Создание отеля токеном USER | `POST` | `/api/hotels` | JWT с ролью `ROLE_USER` | `403 Forbidden` |
| H-CT-5 | Получение списка отелей | `GET` | `/api/hotels` | Без токена (публичный эндпоинт) | `200 OK`, список отелей |
| H-CT-6 | Получение отеля по ID | `GET` | `/api/hotels/{id}` | Токен USER | `200 OK`, отель с агрегацией `totalRooms`, `availableRooms` |
| H-CT-7 | Отель не найден | `GET` | `/api/hotels/999` | Токен USER | `404 Not Found` |
| H-CT-8 | Обновление отеля | `PATCH` | `/api/hotels/{id}` | Токен ADMIN, валидные данные | `200 OK`, обновлённые `name` и `address` |
| H-CT-9 | Удаление отеля | `DELETE` | `/api/hotels/{id}` | Токен ADMIN | `204 No Content` |
| H-CT-10 | Удаление несуществующего отеля | `DELETE` | `/api/hotels/999` | Токен ADMIN | `404 Not Found` |

### 1.2. `RoomControllerTest` — `@WebMvcTest + @AutoConfigureMockMvc`

| # | Тест | Метод | Путь | Что проверяет | Ожидаемый результат |
|---|------|-------|------|---------------|---------------------|
| H-RT-1 | Создание номера | `POST` | `/api/rooms` | Токен ADMIN, валидный `hotelId` | `201 Created`, `hotelId` и `number` в ответе |
| H-RT-2 | Создание номера без токена | `POST` | `/api/rooms` | Отсутствие JWT-токена | `401 Unauthorized` |
| H-RT-3 | Создание номера с `hotelId=999` | `POST` | `/api/rooms` | Не существует отель | `404 Not Found` |
| H-RT-4 | Создание номера без `hotelId` | `POST` | `/api/rooms` | Валидация `@NotNull` | `400 Bad Request` |
| H-RT-5 | Получение доступных номеров | `GET` | `/api/rooms` | Без токена (публичный) | `200 OK`, список `available=true` |
| H-RT-6 | Рекомендованные номера | `GET` | `/api/rooms/recommend` | Без токена (публичный) | `200 OK`, сортировка `timesBooked ASC, id ASC` |
| H-RT-7 | Подтверждение доступности | `POST` | `/api/rooms/{id}/confirm-availability` | Корректные даты, нет пересечений | `200 OK` |
| H-RT-8 | Подтверждение доступности — пересечение | `POST` | `/api/rooms/{id}/confirm-availability` | Есть пересекающееся бронирование | `409 Conflict` |
| H-RT-9 | Снятие блокировки | `POST` | `/api/rooms/{id}/release` | Блокировка существует | `200 OK` |
| H-RT-10 | Снятие блокировки без блокировки | `POST` | `/api/rooms/{id}/release` | Нет активной блокировки | `200 OK` (пустой результат) |

### 1.3. `HotelServiceTest` — `@ExtendWith(MockitoExtension.class)`

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| H-HS-1 | Создание отеля | `createHotel()` | `HotelRepository.save()` вызывается один раз | Возвращается `HotelResponse` с `id` |
| H-HS-2 | Получение всех отелей | `getAllHotels()` | `HotelRepository.findAllWithRooms()` возвращает список | Список `HotelResponse` с агрегацией |
| H-HS-3 | Обновление отеля | `updateHotel(id, request)` | Отель существует, поля обновлены | `HotelResponse` с новыми значениями |
| H-HS-4 | Обновление несуществующего отеля | `updateHotel(999, request)` | `HotelRepository.findById(999)` возвращает `Optional.empty()` | Исключение `ResourceNotFoundException` |
| H-HS-5 | Удаление отеля | `deleteHotel(id)` | `HotelRepository.deleteById(id)` вызывается | Без исключения |
| H-HS-6 | Удаление несуществующего отеля | `deleteHotel(999)` | Отель не найден | Исключение `ResourceNotFoundException` |

### 1.4. `RoomServiceTest` — `@ExtendWith(MockitoExtension.class)`

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| H-RS-1 | Создание номера | `createRoom()` | `RoomRepository.save()` вызывается | `RoomResponse` с корректными полями |
| H-RS-2 | Получение всех доступных номеров | `getAllAvailableRooms()` | `RoomRepository.findByAvailableTrue()` возвращает список | Список номеров, все `available=true` |
| H-RS-3 | Получение рекомендованных номеров | `getRecommendedRooms()` | `RoomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc()` | Сортировка: сначала меньший `timesBooked`, при равенстве — меньший `id` |
| H-RS-4 | Подтверждение доступности — успешно | `confirmAvailability(id, start, end, corrId)` | Нет пересекающихся бронирований | `200 OK`, временное бронирование создано |
| H-RS-5 | Подтверждение доступности — занято | `confirmAvailability(id, start, end, corrId)` | `RoomRepository.findOverlappingBookings()` возвращает список | Исключение `RoomNotAvailableException` |
| H-RS-6 | Подтверждение доступности — номер не найден | `confirmAvailability(999, start, end, corrId)` | `RoomRepository.findById(999)` возвращает `empty()` | Исключение `ResourceNotFoundException` |
| H-RS-7 | Снятие блокировки | `releaseSlot(id, start, end, corrId)` | `BookingRepository.deleteTemporaryBookings()` вызывается | Без исключения |
| H-RS-8 | Подтверждение доступности — непересекающиеся даты | `confirmAvailability(id, pastStart, pastEnd, corrId)` | Нет пересечений с существующими | `200 OK` |

### 1.5. `HotelRepositoryTest` — `@DataJpaTest + TestEntityManager`

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| H-REPO-1 | `findByAvailableTrueOrderByTimesBookedAscIdAsc` | Создать 3 номера с разным `timesBooked` | Сортировка по `timesBooked ASC, id ASC` | Порядок: `timesBooked=0, timesBooked=1, timesBooked=2` |
| H-REPO-2 | `findOverlappingBookings` | Создать бронирование с `start=10:00, end=12:00` | Запрос `11:00–13:00` (пересечение) | Находит 1 пересечение |
| H-REPO-3 | `findOverlappingBookings` — нет пересечения | Создать бронирование `start=10:00, end=12:00` | Запрос `13:00–15:00` (после) | Не находит пересечений |
| H-REPO-4 | `findOverlappingBookings` — нет пересечения (до) | Создать бронирование `start=10:00, end=12:00` | Запрос `08:00–09:00` (до) | Не находит пересечений |
| H-REPO-5 | `findByName` | Создать отель с `name="Grand Hotel"` | Поиск по точному названию | Находит отель |
| H-REPO-6 | `findByNameContainingIgnoreCase` | Создать отель `name="Grand Hotel"` | Поиск `"grand"` | Находит отель |
| H-REPO-7 | `findByIdWithRooms` | Создать отель с 2 номерами | `JOIN FETCH` загружает номера | `rooms.size() == 2` |
| H-REPO-8 | `deleteTemporaryBookings` | Создать временное бронирование (`userId=0`) | Вызов `deleteTemporaryBookings` | 1 запись удалена |

---

## 2. Booking Service

> **Этап реализации:** Этап 16

### 2.1. `AuthControllerTest` — `@WebMvcTest + @AutoConfigureMockMvc`

| # | Тест | Метод | Путь | Что проверяет | Ожидаемый результат |
|---|------|-------|------|---------------|---------------------|
| B-AT-1 | Регистрация нового пользователя | `POST` | `/user/register` | Валидные `username`/`password` | `200 OK`, JWT-токен в ответе |
| B-AT-2 | Регистрация дубликата | `POST` | `/user/register` | Тот же `username` | `409 Conflict` или `400` |
| B-AT-3 | Авторизация — успешно | `POST` | `/user/auth` | Правильные `username`/`password` | `200 OK`, JWT-токен |
| B-AT-4 | Авторизация — неверный пароль | `POST` | `/user/auth` | Неправильный `password` | `401 Unauthorized` |
| B-AT-5 | Авторизация — пользователь не найден | `POST` | `/user/auth` | Несуществующий `username` | `401 Unauthorized` |
| B-AT-6 | Регистрация без тела | `POST` | `/user/register` | Пустой запрос | `400 Bad Request` |

### 2.2. `UserControllerTest` — `@WebMvcTest + @AutoConfigureMockMvc`

| # | Тест | Метод | Путь | Что проверяет | Ожидаемый результат |
|---|------|-------|------|---------------|---------------------|
| B-UCT-1 | Создание пользователя (ADMIN) | `POST` | `/user` | Токен ADMIN, валидные данные | `201 Created`, `role=USER` |
| B-UCT-2 | Создание пользователя — роль ADMIN | `POST` | `/user` | Токен ADMIN, `role=ADMIN` в теле | `201 Created`, `role=ADMIN` |
| B-UCT-3 | Создание пользователя без токена | `POST` | `/user` | Отсутствие JWT | `401 Unauthorized` |
| B-UCT-4 | Создание пользователя токеном USER | `POST` | `/user` | Токен с `ROLE_USER` | `403 Forbidden` |
| B-UCT-5 | Обновление пользователя | `PATCH` | `/user` | Токен ADMIN, валидные данные | `200 OK`, обновлённые поля |
| B-UCT-6 | Обновление несуществующего | `PATCH` | `/user` | `id=999` не существует | `404 Not Found` |
| B-UCT-7 | Удаление пользователя | `DELETE` | `/user` | Токен ADMIN, валидный `id` | `200 OK` или `204` |
| B-UCT-8 | Удаление без токена | `DELETE` | `/user` | Отсутствие JWT | `401 Unauthorized` |

### 2.3. `BookingControllerTest` — `@WebMvcTest + @AutoConfigureMockMvc + моки Feign-клиента`

| # | Тест | Метод | Путь | Что проверяет | Ожидаемый результат |
|---|------|-------|------|---------------|---------------------|
| B-BCT-1 | Создание бронирования (автоподбор) | `POST` | `/booking` | `autoSelect=true`, токен USER | `201 Created`, статус `PENDING` (моки Feign) |
| B-BCT-2 | Создание бронирования (ручной выбор) | `POST` | `/booking` | `autoSelect=false`, `roomId` указан | `201 Created`, статус `PENDING` |
| B-BCT-3 | Создание бронирования без токена | `POST` | `/booking` | Отсутствие JWT | `401 Unauthorized` |
| B-BCT-4 | Создание бронирования токеном ADMIN | `POST` | `/booking` | Токен `ROLE_ADMIN` | `403 Forbidden` |
| B-BCT-5 | История бронирований | `GET` | `/bookings` | Токен USER | `200 OK`, список бронирований пользователя |
| B-BCT-6 | Получение бронирования по ID | `GET` | `/booking/{id}` | Токен USER, своё бронирование | `200 OK` |
| B-BCT-7 | Получение чужого бронирования | `GET` | `/booking/{id}` | Токен USER, чужой `userId` | `403 Forbidden` |
| B-BCT-8 | Бронирование не найдено | `GET` | `/booking/999` | `id=999` не существует | `404 Not Found` |
| B-BCT-9 | Отмена бронирования | `DELETE` | `/booking/{id}` | Токен USER, своё бронирование | `200 OK`, статус `CANCELLED` |
| B-BCT-10 | Отмена чужого бронирования | `DELETE` | `/booking/{id}` | Токен USER, чужое | `403 Forbidden` |
| B-BCT-11 | Отмена несуществующего | `DELETE` | `/booking/999` | `id=999` | `404 Not Found` |

### 2.4. `BookingServiceTest` — `@ExtendWith(MockitoExtension.class)`

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| B-BST-1 | Успешное бронирование (PENDING→CONFIRMED) | `createBooking()` | Моки `FeignClient.confirmAvailability()` возвращает успех | Статус `CONFIRMED`, `releaseSlot` не вызывался |
| B-BST-2 | Ошибка Feign-клиента (PENDING→CANCELLED) | `createBooking()` | Моки `FeignClient.confirmAvailability()` выбрасывает `RemoteException` | Статус `CANCELLED`, `releaseSlot` вызван |
| B-BST-3 | Таймаут Feign-клиента (PENDING→CANCELLED) | `createBooking()` | Моки `FeignClient.confirmAvailability()` выбрасывает `TimeoutException` | Статус `CANCELLED`, `releaseSlot` вызван |
| B-BST-4 | Idempotency — повторный вызов | `createBooking()` с тем же `correlationId` | Уже существует бронирование с таким `correlationId` | Возвращается существующее бронирование, не создаётся новое |
| B-BST-5 | Автоподбор номера | `createBooking(autoSelect=true)` | Моки `hotelServiceClient.getRecommendedRooms()` возвращает список | Выбран первый номер из списка |
| B-BST-6 | Автоподбор — все номера заняты | `createBooking(autoSelect=true)` | Моки возвращает пустой список | Исключение `RoomNotAvailableException` |
| B-BST-7 | Отмена бронирования | `cancelBooking(id, userId)` | Статус меняется на `CANCELLED` | `Booking.status == CANCELLED` |
| B-BST-8 | Получение истории | `getBookingsByUserId(userId)` | `BookingRepository.findByUserId(userId)` | Список бронирований конкретного пользователя |

### 2.5. `AuthServiceTest` — `@ExtendWith(MockitoExtension.class)`

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| B-AST-1 | Регистрация пользователя | `register()` | `userRepository.existsByUsername()` = false, `save()` вызван | Токен сгенерирован, `role=USER` |
| B-AST-2 | Регистрация дубликата | `register()` | `existsByUsername()` = true | Исключение (409) |
| B-AST-3 | Авторизация — успешно | `authenticate()` | `findByUsername()` возвращает пользователя, `passwordEncoder.matches()` = true | JWT-токен с `exp=1h` |
| B-AST-4 | Авторизация — неверный пароль | `authenticate()` | `passwordEncoder.matches()` = false | Исключение (401) |

### 2.6. `BookingRepositoryTest` — `@DataJpaTest + TestEntityManager`

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| B-REPO-1 | `findByUserId` | Создать 2 бронирования одного пользователя | Запрос по `userId` | Возвращает 2 бронирования |
| B-REPO-2 | `findByStatus` | Создать бронирования со статусами PENDING, CONFIRMED, CANCELLED | Запрос `findByStatus(PENDING)` | Возвращает 1 бронирование |
| B-REPO-3 | `existsByRoomIdAndOverlapDates` — есть пересечение | Создать бронирование `10:00–12:00` | Проверка `11:00–13:00` | `true` |
| B-REPO-4 | `existsByRoomIdAndOverlapDates` — нет пересечения | Создать бронирование `10:00–12:00` | Проверка `13:00–15:00` | `false` |
| B-REPO-5 | `existsByUsername` — существует | Создать пользователя с `username="test"` | `existsByUsername("test")` | `true` |
| B-REPO-6 | `existsByUsername` — не существует | `existsByUsername("ghost")` | `existsByUsername("ghost")` | `false` |

### 2.7. `HotelServiceClientTest` — тест Feign-клиента

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| B-HCT-1 | `confirmAvailability` — успех | Моки HTTP 200 | FeignClient возвращает `AvailabilityResponse` | Статус 200, тело распарсено |
| B-HCT-2 | `confirmAvailability` — ошибка 409 | Моки HTTP 409 | Исключение `RoomNotAvailableException` | Исключение с сообщением |
| B-HCT-3 | `confirmAvailability` — ошибка 500 | Моки HTTP 500 | Исключение `RemoteServiceException` (или аналог) | Исключение с кодом 500 |
| B-HCT-4 | `releaseSlot` — успех | Моки HTTP 200 | FeignClient возвращает `ReleaseResponse` | Статус 200 |
| B-HCT-5 | `getRecommendedRooms` — успех | Моки HTTP 200 | Список рекомендованных номеров | Список не пустой |
| B-HCT-6 | `confirmAvailability` — таймаут | Моки `ReadTimeoutException` | Исключение таймаута | Исключение с текстом таймаута |

---

## 3. API Gateway

> **Этап реализации:** Этап 17 (требует запущенных Eureka, Hotel Service и Booking Service)

### 3.1. `ApiGatewayTest` — интеграционные проверки

| # | Тест | Метод | Что проверяет | Ожидаемый результат |
|---|------|-------|---------------|---------------------|
| G-T-1 | Роутинг `/api/hotels/**` | Запрос через Gateway | Маршрутизация в `hotel-service` через Eureka | `200 OK` (если Hotel Service запущен) или корректный fallback |
| G-T-2 | Роутинг `/api/bookings/**` | Запрос через Gateway | Маршрутизация в `booking-service` через Eureka | `200 OK` (если Booking Service запущен) или корректный fallback |
| G-T-3 | Передача JWT-заголовка | Запрос с `Authorization: Bearer <token>` | Заголовок проксируется в бэкенд | Запрос доходит до сервиса с токеном |
| G-T-4 | StripPrefix | Запрос на `/api/hotels/1` | `StripPrefix=1` убирает `/api` | Сервис получает `/hotels/1` |

---

## 4. Eureka Server

Интеграционные тесты не требуются. Сервис проверяется вручную при запуске системы (Этап 3, Этап 17).

---

## Сводная статистика

| Сервис | Кол-во тестов | Этап |
|--------|--------------|------|
| Hotel Service | 44 | 7 |
| Booking Service | 36 | 16 |
| API Gateway | 4 | 17 |
| **Итого** | **84** | — |
