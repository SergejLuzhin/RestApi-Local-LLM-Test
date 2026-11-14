# Что было выполнено

## Этап 1: Настройка родительского POM и создание модулей

### Дата выполнения
14 ноября 2026

### Что сделано

#### 1. Обновлён родительский `pom.xml`

Создан многомодульный Maven-проект с единым управлением зависимостями.

**Ключевые изменения:**
- `packaging` изменён на `pom`
- `artifactId` изменён на `hotel-booking-system`
- Добавлен `description`: "Distributed hotel booking system with Spring Boot microservices"
- Подключены 4 модуля: `eureka-server`, `api-gateway`, `hotel-service`, `booking-service`

**BOM-управление зависимостями:**
- Spring Boot: `3.5.3` (spring-boot-dependencies)
- Spring Cloud: `2025.0.0` (Levi release train)
- Resilience4j: `2.2.0`
- Lombok: `1.18.48` (обновлена с 1.18.36 из-за совместимости с JDK 25)
- MapStruct: `1.6.3`
- SpringDoc OpenAPI: `2.8.9`
- JWT (jjwt): `0.12.6`

**Плагин maven-compiler-plugin:**
- Annotation Processor paths в порядке: Lombok → MapStruct → Lombok-MapStruct binding
- Используется `--release 17` вместо `source/target` для корректной работы на JDK 25
- Исключён Lombok из spring-boot-maven-plugin

**Общие зависимости (scope=provided/test):**
- Lombok (provided)
- MapStruct (provided)
- Lombok-MapStruct binding (provided)
- Spring Boot Starter Test (test)

#### 2. Созданы 4 модуля проекта

Каждый модуль содержит:
- `pom.xml` с наследованием от parent
- Базовая структура пакетов в `src/main/java/...`
- `src/main/resources/application.yml`
- `*Application.java` — точка входа с `@SpringBootApplication`

**Структура пакетов:**
- `hotel-service`: `entity`, `repository`, `service`, `controller`, `dto`, `mapper`, `config`, `exception`
- `booking-service`: `entity`, `repository`, `service`, `controller`, `dto`, `mapper`, `config`, `exception`, `client`
- `api-gateway`: базовый пакет
- `eureka-server`: базовый пакет

#### 3. Конфигурация `application.yml` для каждого модуля

**Eureka Server (`eureka-server/src/main/resources/application.yml`):**
- Порт: `8761`
- `register-with-eureka: false`
- `fetch-registry: false`
- Self-preservation отключён, интервал очистки 5000 мс

**API Gateway (`api-gateway/src/main/resources/application.yml`):**
- Порт: `8080`
- Discovery locator: включён, lower-case-service-id
- Роуты:
  - `/api/bookings/**` → `lb://booking-service` (StripPrefix=1)
  - `/api/hotels/**` → `lb://hotel-service` (StripPrefix=1)
- Eureka Client, Actuator (health, info, gateway)

**Hotel Service (`hotel-service/src/main/resources/application.yml`):**
- Порт: `8081`
- H2 in-memory: `jdbc:h2:mem:hoteldb`
- `ddl-auto: update`
- H2 Console включён на `/h2-console`
- JWT секрет из переменной окружения, TTL: 3600000 мс (1 час)
- Eureka Client, Actuator

**Booking Service (`booking-service/src/main/resources/application.yml`):**
- Порт: `8082`
- H2 in-memory: `jdbc:h2:mem:bookingdb`
- `ddl-auto: update`
- H2 Console включён на `/h2-console`
- JWT секрет из переменной окружения, TTL: 3600000 мс (1 час)
- Eureka Client, Actuator
- Resilience4j:
  - Retry: 3 попытки, 500ms база, экспоненциальный множитель 2
  - Circuit Breaker: sliding window 10, threshold 50%

#### 4. Main-классы приложений

- **EurekaServerApplication**: `@EnableEurekaServer`
- **ApiGatewayApplication**: `@EnableDiscoveryClient`
- **HotelServiceApplication**: `@EnableDiscoveryClient`
- **BookingServiceApplication**: `@EnableDiscoveryClient`, `@EnableFeignClients`

### Проверки

| Проверка | Результат |
|----------|-----------|
| `mvn clean compile` — все модули | ✅ BUILD SUCCESS |
| Eureka Server запущен на порту 8761 | ✅ |
| `curl http://localhost:8761/` — HTML ответ | ✅ |
| `curl http://localhost:8761/eureka/apps` — XML apps | ✅ |

### Замечания

- JDK в системе: **OpenJDK 25.0.1**. Версия Lombok обновлена до `1.18.48` для совместимости.
- Используется `--release 17` для компиляции, что корректно работает на JDK 25.
- Eureka Server завершился с exit code 143 (SIGTERM) — это нормальное поведение при принудительной остановке. Ошибки `EurekaJersey3Client` при shutdown — не критичны.

### Файлы созданы/изменены

| Файл | Действие |
|------|----------|
| `pom.xml` | Изменён (родительский) |
| `eureka-server/pom.xml` | Создан |
| `api-gateway/pom.xml` | Создан |
| `hotel-service/pom.xml` | Создан |
| `booking-service/pom.xml` | Создан |
| `eureka-server/src/main/resources/application.yml` | Создан |
| `api-gateway/src/main/resources/application.yml` | Создан |
| `hotel-service/src/main/resources/application.yml` | Создан |
| `booking-service/src/main/resources/application.yml` | Создан |
| `eureka-server/.../EurekaServerApplication.java` | Создан |
| `api-gateway/.../ApiGatewayApplication.java` | Создан |
| `hotel-service/.../HotelServiceApplication.java` | Создан |
| `booking-service/.../BookingServiceApplication.java` | Создан |

---

## Этап 3: Eureka Server

### Дата выполнения
14 ноября 2026

### Что сделано

По плану Этапа 3:
- **EurekaServerApplication.java** — `@SpringBootApplication` + `@EnableEurekaServer` (уже было из Этапа 2)
- **application.yml** — порт `8761`, `register-with-eureka: false`, `fetch-registry: false`, `enable-self-preservation: false` (уже было из Этапа 2)
- **application-test.yml** (опционально по плану) — создан для тестов на изолированном порту `8762`

### Проверки

| Проверка | Результат |
|----------|-----------|
| `mvn clean compile` — модуль eureka-server | ✅ BUILD SUCCESS |
| `mvn spring-boot:run` — запуск сервера | ✅ Успешный запуск за 2.034 сек |
| `curl http://localhost:8761/` | ✅ HTTP 200, HTML ответ |
| `curl http://localhost:8761/eureka/apps` | ✅ HTTP 200, XML ответ: `<applications>` |
| `curl http://localhost:8761/eureka/apps.json` | ⚠️ HTTP 404 — не критично (REST API Eureka использует другой путь) |

### Логи запуска (ключевые строки)
```
Starting EurekaServerApplication using Java 25.0.1
Tomcat started on port 8761 (http) with context path '/'
Started EurekaServerApplication in 2.034 seconds
Started Eureka Server
```

### Предупреждения в логах (не критичные)
- `Failed to set up a Bean Validation provider` — не требуется для Eureka Server
- `Spring Cloud LoadBalancer is currently working with the default cache` — рекомендация по Caffeine в production

### Замечания
- JDK в системе: **OpenJDK 25.0.1** — совместимость подтверждена
- Eureka Server работает корректно, готов к регистрации микросервисов

### Файлы созданные/изменённые на этом этапе

| Файл | Действие |
|------|----------|
| `eureka-server/src/test/resources/application-test.yml` | Создан (порт 8762 для тестов) |

---

## Этап 4: Hotel Service — сущности и репозитории

### Дата выполнения
14 ноября 2026

### Что сделано

#### 1. Обновлён `hotel-service/pom.xml`

Добавлена зависимость `spring-boot-starter-validation` для валидации входных данных (DTO).

**Добавленная зависимость:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### 2. Созданы сущности JPA

**BaseEntity** — базовый класс с общими полями для всех сущностей:
- `createdAt` (LocalDateTime) — время создания записи
- `updatedAt` (LocalDateTime) — время последнего обновления
- Методы `@PrePersist` (onCreate) и `@PreUpdate` (onUpdate) для автозаполнения

**Hotel** — сущность «Отель»:
- `id` (Long, auto-generated)
- `name` (String, nullable=false, length=255)
- `address` (String, nullable=false, length=500)
- `rooms` (List<Room>, OneToMany, cascade=ALL, orphanRemoval)
- Методы `addRoom()` и `removeRoom()` для управления связью

**Room** — сущность «Номер»:
- `id` (Long, auto-generated)
- `hotel` (Hotel, ManyToOne, LAZY)
- `number` (String, nullable=false, length=50) — обозначение комнаты
- `available` (Boolean, default=true) — операционная доступность
- `timesBooked` (Integer, default=0) — счётчик бронирований для алгоритма равномерной загрузки

**Booking** (локальная сущность Hotel Service) — сущность для проверки доступности:
- `id` (Long, auto-generated)
- `userId` (Long) — идентификатор пользователя (не foreign key, данные в Booking Service)
- `roomId` (Long) — идентификатор номера
- `startDate` (LocalDateTime) — дата начала бронирования
- `endDate` (LocalDateTime) — дата окончания бронирования
- `status` (BookingStatus enum: PENDING, CONFIRMED, CANCELLED)
- Используется исключительно в Hotel Service для проверки пересечений дат

#### 3. Созданы репозитории

**HotelRepository** — JpaRepository<Hotel, Long>:
- `findByName(String name)` — поиск по точному названию
- `findByNameContainingIgnoreCase(String name)` — поиск по подстроке
- `findAllWithRooms()` — JOIN FETCH для предотвращения N+1
- `findByIdWithRooms(Long id)` — поиск по ID с номерами

**RoomRepository** — JpaRepository<Room, Long>:
- `findByAvailableTrue()` — все доступные номера
- `findByAvailableTrueOrderByTimesBookedAscIdAsc()` — доступные, сортировка по timesBooked ASC, id ASC (алгоритм автоподбора)
- `findByHotelIdAndAvailableTrue(Long hotelId)` — доступные номера конкретного отеля
- `findOverlappingBookings(roomId, start, end)` — проверка пересечений бронирований (PENDING/CONFIRMED)
- `findByIdWithHotel(Long id)` — поиск с загруженным отелем

#### 4. Созданы DTO

**HotelCreateRequest** (запрос создания отеля):
- `name` (NotBlank, max 255)
- `address` (NotBlank, max 500)

**HotelResponse** (ответ с информацией об отеле):
- `id`, `name`, `address`, `totalRooms`, `availableRooms`
- `createdAt`, `updatedAt`

**RoomCreateRequest** (запрос создания номера):
- `hotelId` (NotNull, Min 1)
- `number` (NotBlank, max 50)
- `available` (Boolean, default=true)

**RoomResponse** (ответ с информацией о номере):
- `id`, `hotelId`, `hotelName`, `number`, `available`, `timesBooked`
- `createdAt`, `updatedAt`

#### 5. Созданы MapStruct-мапперы

**HotelMapper** (`@Mapper(componentModel = "spring")`):
- `toEntity(HotelCreateRequest)` — создание сущности из запроса
- `toResponse(Hotel)` — преобразование в DTO с вычислением totalRooms и availableRooms
- `toResponseList(List<Hotel>)` — преобразование списка

**RoomMapper** (`@Mapper(componentModel = "spring")`):
- `toEntity(RoomCreateRequest)` — создание сущности из запроса
- `toResponse(Room)` — преобразование в DTO с извлечением hotel.id и hotel.name
- `toResponseList(List<Room>)` — преобразование списка

### Проверки

| Проверка | Результат |
|----------|-----------|
| `mvn clean compile` — модуль hotel-service | ✅ BUILD SUCCESS |
| `mvn spring-boot:run` — запуск сервиса | ✅ Успешный запуск за 3.806 сек |
| `curl http://localhost:8081/api/hotels` (без токена) | ✅ HTTP 401 (ожидается, безопасность в Этапе 6) |
| Логи H2 инициализации | ✅ Таблицы hotels, rooms, bookings, base_entity созданы |

### Логи запуска (ключевые строки)
```
Starting HotelServiceApplication using Java 25.0.1
...
Tomcat started on port 8081 (http) with context path '/'
Started HotelServiceApplication in 3.806 seconds
```

### Предупреждения в логах (не критичные)
- `Cannot execute request on any known server` — ожидаемо, Eureka Server не запущен во время тестирования. В финальной проверке (Этап 17) все сервисы будут запущены одновременно.

### Замечания
- JDK в системе: **OpenJDK 25.0.1** — совместимость подтверждена
- Сущность `Booking` создана локально в Hotel Service только для проверки пересечений дат. Полная сущность Booking находится в Booking Service
- Lombok-генераторы работают корректно, MapStruct генерирует реализацию без ошибок
- Для MapStruct-мапперов использован `unmappedTargetPolicy = IGNORE`, чтобы игнорировать поля, не отображаемые через Lombok-билдер

### Файлы созданные/изменённые на этом этапе

| Файл | Действие |
|------|----------|
| `hotel-service/pom.xml` | Изменён (добавлена validation-зависимость) |
| `hotel-service/.../entity/BaseEntity.java` | Создан |
| `hotel-service/.../entity/Hotel.java` | Создан |
| `hotel-service/.../entity/Room.java` | Создан |
| `hotel-service/.../entity/Booking.java` | Создан |
| `hotel-service/.../repository/HotelRepository.java` | Создан |
| `hotel-service/.../repository/RoomRepository.java` | Создан |
| `hotel-service/.../dto/HotelCreateRequest.java` | Создан |
| `hotel-service/.../dto/HotelResponse.java` | Создан |
| `hotel-service/.../dto/RoomCreateRequest.java` | Создан |
| `hotel-service/.../dto/RoomResponse.java` | Создан |
| `hotel-service/.../mapper/HotelMapper.java` | Создан |
| `hotel-service/.../mapper/RoomMapper.java` | Создан |

### Следующий этап
**Этап 5: Hotel Service — сервисы и контроллеры (CRUD)**

---

## Этап 5: Hotel Service — сервисы и контроллеры (CRUD)

### Дата выполнения
15 ноября 2026

### Что сделано

#### 1. Созданы исключения

**RoomNotAvailableException** — исключение для кода 409 Conflict при невозможности подтверждения доступности номера.
```
RoomNotAvailableException(Long roomId, String message)
RoomNotAvailableException(Long roomId, Throwable cause)
```

**ResourceNotFoundException** — исключение для кода 404 Not Found при отсутствии ресурса.
```
ResourceNotFoundException(String resourceName, Long id)
```

**GlobalExceptionHandler** — глобальный обработчик исключений (`@RestControllerAdvice`):
- `ResourceNotFoundException` → HTTP 404 Not Found
- `RoomNotAvailableException` → HTTP 409 Conflict
- `AccessDeniedException` → HTTP 403 Forbidden
- `MethodArgumentNotValidException` → HTTP 400 Bad Request с детализацией ошибок по полям
- `Exception` (generic) → HTTP 500 Internal Server Error

Все ошибки возвращаются в формате JSON с полями: `timestamp`, `status`, `error`, `message`, `fieldErrors`.

#### 2. Созданы сервисы

**HotelService** — бизнес-логика CRUD для отелей:
- `createHotel(HotelCreateRequest)` — создание отеля со статусом 201 Created
- `getAllHotels()` — получение всех отелей с агрегацией totalRooms и availableRooms
- `getHotelById(Long)` — получение отеля по ID
- `updateHotel(Long, HotelCreateRequest)` — обновление названия и адреса
- `deleteHotel(Long)` — каскадное удаление отеля со всеми номерами

**RoomService** — бизнес-логика для номеров + доступность:
- `createRoom(RoomCreateRequest)` — создание номера в отеле (сохранение напрямую через RoomRepository для корректного получения ID)
- `getAllAvailableRooms()` — получение всех доступных номеров (available = true)
- `getRecommendedRooms()` — получение номеров для автоподбора (сортировка: timesBooked ASC, id ASC)
- `getRoomById(Long)` — получение номера по ID
- `confirmAvailability(Long roomId, LocalDateTime start, LocalDateTime end, String correlationId)` — подтверждение доступности:
  - Проверяет существование номера
  - Проверяет пересечение дат с существующими бронированиями (PENDING/CONFIRMED)
  - При пересечении выбрасывает `RoomNotAvailableException` (409)
  - При отсутствии пересечений создаёт временную блокировку (локальное бронирование с userId=0)
- `releaseSlot(Long roomId, LocalDateTime start, LocalDateTime end, String correlationId)` — компенсация:
  - Удаляет временное бронирование (userId=0) для заданного периода через `@Modifying` запрос

#### 3. Созданы контроллеры

**HotelController** (`@RequestMapping("/api/hotels")`):
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/hotels` | Создать отель (201) |
| `GET` | `/api/hotels` | Получить список отелей |
| `GET` | `/api/hotels/{id}` | Получить отель по ID |
| `PATCH` | `/api/hotels/{id}` | Обновить отель |
| `DELETE` | `/api/hotels/{id}` | Удалить отель (204) |

**RoomController** (`@RequestMapping("/api/rooms")`):
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/rooms` | Создать номер (201) |
| `GET` | `/api/rooms` | Получить доступные номера |
| `GET` | `/api/rooms/recommend` | Получить рекомендованные номера (timesBooked ASC) |
| `POST` | `/api/rooms/{roomId}/confirm-availability` | Подтвердить доступность (200) |
| `POST` | `/api/rooms/{roomId}/release` | Снять временную блокировку (200) |

Все эндпоинты аннотированы SpringDoc OpenAPI: `@Operation`, `@ApiResponse`, `@Tag`.

#### 4. Создан репозиторий BookingRepository

**BookingRepository** — операции с локальными бронированиями в Hotel Service:
- `findOverlappingBookings(roomId, start, end)` — поиск пересекающихся бронирований
- `findTemporaryBookings(roomId, start, end)` — поиск временных бронирований (userId=0)
- `deleteTemporaryBookings(roomId, start, end)` — удаление временных бронирований (`@Modifying`)

#### 5. Добавлены `-parameters` в компилятор

В родительский `pom.xml` добавлен компиляторный флаг `<arg>-parameters</arg>`, чтобы Spring MVC мог корректно обрабатывать `@PathVariable` и `@RequestParam` параметры.

#### 6. Временная конфигурация безопасности

Создан `SecurityConfig` для тестирования Этапа 5:
- `@EnableWebSecurity` (без `@EnableMethodSecurity`, чтобы `@PreAuthorize` не блокировал запросы)
- `anyRequest().permitAll()` — все эндпоинты доступны без аутентификации

> **Примечание**: Полная JWT-безопасность будет реализована в Этапе 6.

### Проверки

Все 12 тестов прошли успешно:

| Тест | Метод | Путь | Результат |
|------|-------|------|-----------|
| 1 | POST | /api/hotels | ✅ HTTP 201, id=1 |
| 2 | POST | /api/rooms | ✅ HTTP 201, id=1, hotelId=1, hotelName="Grand Hotel" |
| 3 | POST | /api/rooms (room 201) | ✅ HTTP 201, id=2 |
| 4 | GET | /api/hotels | ✅ Список из 1 отеля, totalRooms=2, availableRooms=2 |
| 5 | GET | /api/hotels/1 | ✅ HTTP 200, отель с деталями |
| 6 | GET | /api/rooms | ✅ Список из 2 номеров |
| 7 | GET | /api/rooms/recommend | ✅ Сортировка timesBooked ASC, id ASC |
| 8 | POST | /api/rooms/1/confirm-availability | ✅ HTTP 200, слот заблокирован |
| 9 | POST | /api/rooms/1/confirm-availability (дубликат) | ✅ HTTP 409 Conflict |
| 10 | POST | /api/rooms/1/release | ✅ HTTP 200, блокировка снята |
| 11 | PATCH | /api/hotels/1 | ✅ HTTP 200, отель обновлён |
| 12 | DELETE | /api/hotels/1 | ✅ HTTP 204 |
| — | DELETE | /api/hotels/999 | ✅ HTTP 404 Not Found |
| — | POST | /api/hotels (пустое тело) | ✅ HTTP 400, валидация name + address |
| — | POST | /api/rooms (hotelId=999) | ✅ HTTP 404, отель не найден |

**Дополнительные сценарии:**
- ✅ Подтверждение доступности после release — успешно (HTTP 200)
- ✅ Release несуществующего слота — успешно (HTTP 200, пустой результат)
- ✅ Подтверждение для непересекающегося периода — успешно (HTTP 200)

### Логи компиляции

```
mvn clean compile — BUILD SUCCESS
Compiling 22 source files
Started HotelServiceApplication in 2.601 seconds
```

### Замечания

- JDK в системе: **OpenJDK 25.0.1** — совместимость подтверждена
- Eureka Server не запущен — ошибки `Connect to http://localhost:8761 failed: Connection refused` ожидаемы
- H2 in-memory — данные сбрасываются при перезапуске (норма для разработки)
- MapStruct `toResponse(Room)` корректно маппит `hotel.id` → `hotelId` и `hotel.name` → `hotelName`
- Валидация через `@NotBlank`/`@NotNull` работает, ошибки возвращаются с русскими сообщениями
- `@Modifying` запрос в `BookingRepository` требует `@Transactional` на вызывающем методе
- Фикс: `@PathVariable` имя должно совпадать с параметром метода (`{roomId}` → `roomId`)

### Файлы созданные/изменённые на этом этапе

| Файл | Действие |
|------|----------|
| `hotel-service/.../exception/RoomNotAvailableException.java` | Создан |
| `hotel-service/.../exception/ResourceNotFoundException.java` | Создан |
| `hotel-service/.../exception/GlobalExceptionHandler.java` | Создан |
| `hotel-service/.../service/HotelService.java` | Создан |
| `hotel-service/.../service/RoomService.java` | Создан |
| `hotel-service/.../controller/HotelController.java` | Создан |
| `hotel-service/.../controller/RoomController.java` | Создан |
| `hotel-service/.../repository/BookingRepository.java` | Создан |
| `hotel-service/.../config/SecurityConfig.java` | Создан (временный, для тестов Этапа 5) |
| `pom.xml` | Изменён (добавлен `<arg>-parameters</arg>`) |

### Следующий этап
**Этап 6: Hotel Service — безопасность и Swagger**
