# План реализации микросервисного приложения бронирования отелей

---

## Правила и ограничения

1. **Отвечать строго на русском языке**
2. **Для каждого метода и класса писать подробный JavaDoc на русском языке**
3. **Весь код строго на английском языке**, включая наименования методов, переменных, а также логи и тексты ошибок
4. **Не переходить к реализации следующего шага без команды пользователя**
5. **После реализации каждого шага автоматически проводить проверку**: код компилируется, запускается, функционал отрабатывает как задумывалось
6. **После реализации каждого шага и проверок логировать весь прогресс в файл `what-was-done.md`** — строго по команде пользователя

---

## Архитектура проекта

Многомодульный Maven-проект:
```
RestApi-Local-LLM-Test/
├── pom.xml                         (parent pom — BOM-управление зависимостями)
├── eureka-server/                  (Eureka Service Registry)
├── api-gateway/                    (Spring Cloud Gateway)
├── hotel-service/                  (Hotel Management Service)
├── booking-service/                 (Booking Service)
```

**Версии**: Spring Boot 3.5.x, Spring Cloud 2025.0.x (Levi), Java 17, H2, JWT (jjwt 0.12.x), Lombok, MapStruct, SpringDoc OpenAPI 2.x.

---

## ЭТАП 1. Настройка родительского POM

**Цель**: Создать мульти-модульный Maven-проект с единым управлением зависимостями.

### Что создаётся:
- Обновлённый `pom.xml` с:
  - `packaging` → `pom`
  - `modules`: `eureka-server`, `api-gateway`, `hotel-service`, `booking-service`
  - `<dependencyManagement>`: Spring Boot BOM + Spring Cloud BOM
  - Общие версии: `spring-boot.version`, `spring-cloud.version`, `jjwt.version`, `lombok.version`, `mapstruct.version`, `springdoc.version`
  - `<plugins>` в `<pluginManagement>`: `spring-boot-maven-plugin`, `maven-compiler-plugin` с Annotation Processor Settings (Lombok + MapStruct)
  - Общие `<dependencies>`: Lombok, MapStruct, Spring Boot Test (scope=test)

**Проверка**: `mvn clean compile -N` (проверка parent-проекта без подмодулей).

---

## ЭТАП 2. Создание модулей (скелет)

**Цель**: Создать все 4 модуля с базовой структурой.

### Что создаётся:
- Папки: `eureka-server/pom.xml`, `api-gateway/pom.xml`, `hotel-service/pom.xml`, `booking-service/pom.xml`
- Каждый модульный POM: `parent` → `../pom.xml`, свой `artifactId`, зависимости первого шага
- Базовая структура пакетов для каждого модуля:
  ```
  src/main/java/com/skillfactory/luzhin/{module}/
    {Module}Application.java
  src/main/resources/application.yml
  ```

**Проверка**: `mvn clean compile -pl eureka-server,api-gateway,hotel-service,booking-service -am`

---

## ЭТАП 3. Eureka Server

**Цель**: Запустить сервер регистрации сервисов.

### Что создаётся:
- **EurekaServerApplication.java** — `@SpringBootApplication` + `@EnableEurekaServer`
- **application.yml** — порт `8761`, `eureka.client.register-with-eureka=false`, `eureka.client.fetch-registry=false`, отключение авторизации
- **application-test.yml** (опционально) — для тестов

**Проверка**: `mvn spring-boot:run` в модуле `eureka-server`, проверка что `/eureka/apps` доступен на порту 8761.

---

## ЭТАП 4. Hotel Service — сущности и репозитории

**Цель**: Создать базовые сущности JPA и репозитории Hotel Service.

### Что создаётся:
- **pom.xml** модуля: Spring Boot Starter Web, Spring Data JPA, H2, Lombok, MapStruct, SpringDoc, Eureka Client
- **HotelServiceApplication.java** — `@SpringBootApplication` + `@EnableDiscoveryClient`
- **Entity Hotel** — `id`, `name`, `address`, `@OneToMany Room`
- **Entity Room** — `id`, `hotel` (ManyToOne), `number` (номер комнаты), `available` (boolean), `timesBooked` (int)
- **Entity BaseEntity** (optional common fields: `createdAt`, `updatedAt`)
- **Repository HotelRepository** — `JpaRepository<Hotel, Long>` + методы поиска по имени
- **Repository RoomRepository** — `JpaRepository<Room, Long>` + кастомные методы:
  - `findByAvailableTrue()`
  - `findByAvailableTrueOrderByTimesBookedAscIdAsc()`
  - `countByHotelIdAnd...` (для статистики)
- **DTO HotelCreateRequest**, **HotelResponse**, **RoomCreateRequest**, **RoomResponse** (MapStruct-маппинг)
- **Mapper HotelMapper**, **RoomMapper** (MapStruct interfaces)

**Проверка**: `mvn clean compile` — нет ошибок компиляции, Lombok-генераторы работают, MapStruct генерирует реализацию.

---

## ЭТАП 5. Hotel Service — сервисы и контроллеры (CRUD)

**Цель**: Реализовать CRUD-операции для отелей и номеров.

### Что создаётся:
- **Service HotelService**:
  - `createHotel(HotelCreateRequest)` → `Hotel`
  - `getAllHotels()` → `List<HotelResponse>`
  - `getHotelById(Long)` → `HotelResponse`
  - `updateHotel(Long, HotelCreateRequest)` → `HotelResponse`
  - `deleteHotel(Long)` — удаление отеля каскадно с номерами
- **Service RoomService**:
  - `createRoom(RoomCreateRequest)` → `RoomResponse`
  - `getAllAvailableRooms()` → `List<RoomResponse>` (только доступные)
  - `getRecommendedRooms()` → `List<RoomResponse>` (доступные, сортировка `timesBooked ASC, id ASC`)
  - `getRoomById(Long)` → `RoomResponse`
  - `confirmAvailability(Long roomId, LocalDateTime start, LocalDateTime end, String correlationId)` — проверка пересечения бронирований, временная блокировка
  - `releaseSlot(Long roomId, LocalDateTime start, LocalDateTime end)` — снятие блокировки
- **Controller HotelController**: `POST /api/hotels`, `GET /api/hotels`
- **Controller RoomController**: `POST /api/rooms`, `GET /api/rooms`, `GET /api/rooms/recommend`, `POST /api/rooms/{id}/confirm-availability`, `POST /api/rooms/{id}/release`

**Детали confirm-availability**:
- Проверяет пересечение заданных дат с существующими бронированиями (через RoomRepository: `findByRoomIdAnd...`)
- Если пересечение есть — выбрасывает `RoomNotAvailableException`
- При отсутствии пересечений — возвращает успешный ответ
- Блокировка на уровне: проверяет наличие `PENDING` или `CONFIRMED` бронирований с пересекающимися датами

**Проверка**: `mvn spring-boot:run`, тесты через `curl` или Postman:
- `POST /api/hotels` — создать отель
- `POST /api/rooms` — создать номер
- `GET /api/hotels` — получить список
- `GET /api/rooms` — получить свободные номера

---

## ЭТАП 6. Hotel Service — безопасность и Swagger

**Цель**: Добавить JWT-валидацию и OpenAPI-документацию.

### Что создаётся:
- **SecurityConfig** — `@EnableWebSecurity`, `@EnableMethodSecurity`:
  - `SecurityFilterChain` с `csrf().disable()`, `sessionManagement().sessionCreationPolicy(STATELESS)`
  - `JwtAuthenticationFilter` — извлекает `Authorization: Bearer <token>`, валидирует JWT
  - `@PreAuthorize("hasRole('ADMIN')")` на админские эндпоинты
- **JwtUtil** — методы: `parseToken`, `getUsername`, `getRole`, `isTokenValid`
- **Application.yml**: JWT secret (хранить в переменной окружения), ключ для валидации
- **OpenAPIConfig** — SpringDoc конфигурация, группировка API
- `@Operation`, `@ApiResponse` аннотации на контроллерах

**Важно**: В Hotel Service JWT-секрет **такой же**, как и в Booking Service (общий секрет для всей системы).

**Проверка**:
- `mvn clean compile`
- Запуск сервиса, запрос без токена → 401
- Запрос с невалидным токеном → 401
- Запрос с валидным токеном USER к админскому эндпоинту → 403
- Запрос с валидным токеном ADMIN → 200

---

## ЭТАП 7. Hotel Service — интеграционные тесты

**Цель**: Написать тесты для CRUD и доступности номеров.

### Что создаётся:
- **HotelControllerTest** — `@WebMvcTest` + `@AutoConfigureMockMvc`:
  - CRUD отели, CRUD номера
  - Проверка авторизации (401 без токена, 403 без ADMIN роли)
- **RoomServiceTest** — `@ExtendWith(MockitoExtension.class)`:
  - Тест `confirmAvailability` — успешный сценарий
  - Тест `confirmAvailability` — номер занят (409 Conflict)
  - Тест `releaseSlot`
- **HotelRepositoryTest** — `@DataJpaTest`:
  - Проверка `findByAvailableTrueOrderByTimesBookedAscIdAsc`
  - Проверка поиска пересечений по датам

**Проверка**: `mvn test` — все тесты зелёные.

---

## ЭТАП 8. Booking Service — сущности, репозитории, DTO

**Цель**: Создать базовые модели и маппинг Booking Service.

### Что создаётся:
- **pom.xml** модуля: Spring Boot Starter Web, Spring Data JPA, H2, Lombok, MapStruct, SpringDoc, Eureka Client, OpenFeign, Resilience4j (retry + circuit breaker)
- **BookingServiceApplication.java** — `@SpringBootApplication` + `@EnableDiscoveryClient` + `@EnableFeignClients`
- **Entity User** — `id`, `username`, `password`, `role` (enum USER/ADMIN)
- **Entity Booking** — `id`, `user` (ManyToOne), `roomId` (Long), `startDate`, `endDate`, `status` (enum PENDING/CONFIRMED/CANCELLED), `createdAt`, `correlationId` (String)
- **Enum BookingStatus**: `PENDING`, `CONFIRMED`, `CANCELLED`
- **Enum UserRole**: `USER`, `ADMIN`
- **Repository UserRepository** — `findByUsername(String)`, `existsByUsername(String)`
- **Repository BookingRepository** — `findByUserId(Long)`, `findByStatus(BookingStatus)`, кастомный `existsByRoomIdAndOverlapDates(roomId, start, end)` для проверки пересечений
- **DTO**: `UserRegisterRequest`, `UserLoginRequest`, `UserResponse`, `UserUpdateRequest`, `BookingCreateRequest`, `BookingResponse`
- **Mapper**: `UserMapper`, `BookingMapper`

**Проверка**: `mvn clean compile`.

---

## ЭТАП 9. Booking Service — JWT и безопасность

**Цель**: Реализовать аутентификацию и авторизацию.

### Что создаётся:
- **SecurityConfig** — аналогично Hotel Service:
  - Stateless JWT-валидация
  - Общий секрет JWT (из переменной окружения `JWT_SECRET`)
- **JwtUtil** — генерация токена (`setExpiration(1h)`), парсинг, валидация, извлечение роли
- **SecurityFilterChain** — разрешённые эндпоинты: `POST /user/register`, `POST /user/auth` (без токена), все остальные — с аутентификацией
- `@PreAuthorize` аннотации на контроллерах:
  - `/booking/**` — `hasRole('USER')`
  - `/user` (кроме register/auth) — `hasRole('ADMIN')`

**Проверка**: `mvn clean compile`.

---

## ЭТАП 10. Booking Service — сервисы (без Feign)

**Цель**: Реализовать бизнес-логику без удалённых вызовов.

### Что создаётся:
- **Service AuthService**:
  - `register(UserRegisterRequest)` — проверка уникальности username, хеширование пароля (`BCryptPasswordEncoder`), создание USER роли
  - `authenticate(UserLoginRequest)` — проверка пароля, генерация JWT-токена
- **Service UserService**:
  - `createUser(UserRegisterRequest)` — ADMIN: создаёт пользователя с указанной ролью
  - `updateUser(Long, UserUpdateRequest)` — ADMIN
  - `deleteUser(Long)` — ADMIN
- **Service BookingService**:
  - `createBooking(BookingCreateRequest, String userId)` — **скелет**: сохраняет PENDING, пока без вызова Hotel Service
  - `getBookingsByUserId(Long userId)` → `List<BookingResponse>`
  - `getBookingById(Long bookingId, Long userId)` → `BookingResponse`
  - `cancelBooking(Long bookingId, Long userId)` — меняет статус на CANCELLED

**Важно**: Пароль хранится в зашифрованном виде (BCrypt).

**Проверка**: `mvn spring-boot:run`, тесты:
- `POST /user/register` — создание пользователя
- `POST /user/auth` — вход, получение токена
- `GET /bookings` — пустой список

---

## ЭТАП 11. Booking Service — Feign-клиент для Hotel Service

**Цель**: Настроить клиент для общения с Hotel Service.

### Что создаётся:
- **FeignClient HotelServiceClient**:
  - `POST /api/rooms/{id}/confirm-availability` → `AvailabilityResponse`
  - `POST /api/rooms/{id}/release` → `ReleaseResponse`
- **FeignConfig**:
  - Настройка тайм-аутов (connect 5s, read 10s)
  - Добавление JWT-токена в заголовок `Authorization` через `RequestInterceptor`
- **RetryConfig** (Resilience4j):
  - `@Retry` аннотация с конфигурацией: max attempts = 3, fixed delay = 500ms (или exponential backoff)
- **Error decoder** для Feign — маппинг ошибок 4xx/5xx в исключения

**Проверка**: `mvn clean compile`.

---

## ЭТАП 12. Booking Service — двухшаговое бронирование (основная логика)

**Цель**: Реализовать распределённую согласованность бронирования.

### Что создаётся:
- **Service BookingService** — обновление метода `createBooking`:

```
createBooking(request, userId):
  1. correlationId = UUID.randomUUID().toString()
  2. Логируем начало процесса с correlationId
  3. @Transactional
     a. Найти пользователя по userId
     b. Создать Booking со статусом PENDING, сохранить
     c. Логируем: "Booking created in PENDING status"
  4. Попытка (retry с ограничением):
     a. Вызов hotelServiceClient.confirmAvailability(roomId, start, end, correlationId)
     b. Если успех → @Transactional:
        - Обновить статус на CONFIRMED
        - Логируем: "Booking CONFIRMED"
     c. Если исключение (timeout, 5xx, 4xx):
        - @Transactional:
          - Обновить статус на CANCELLED
          - Попытка вызвать hotelServiceClient.releaseSlot(roomId, start, end)
          - Логируем: "Booking CANCELLED, compensation attempted"
```

- **Idempotency key**: в таблице бронирований использовать `correlationId`. Перед созданием бронирования проверять, не существует ли уже бронирование с таким `correlationId` (для повторов при retry).
- **Сортировка номеров** при `autoSelect=true`: запрос к Hotel Service `GET /api/rooms/recommend`, выбрать первый свободный.
- **Потоконебезопасность**: все операции в транзакциях, `@Transactional` на каждом методе, мьютексы/синхронизация по `roomId` при конкурентных запросах.

**Проверка**: `mvn clean compile`, запуск обоих сервисов, цепочка:
1. Создать отель + номер в Hotel Service (через Gateway)
2. Зарегистрировать пользователя
3. Создать бронирование → статус PENDING → CONFIRMED

---

## ЭТАП 13. Booking Service — контроллеры

**Цель**: Реализовать REST-контроллеры с документацией.

### Что создаётся:
- **AuthController**:
  - `POST /user/register` — регистрация
  - `POST /user/auth` — авторизация
- **UserController** (`@PreAuthorize("hasRole('ADMIN')")`):
  - `POST /user` — создание
  - `PATCH /user` — обновление
  - `DELETE /user` — удаление
- **BookingController** (`@PreAuthorize("hasRole('USER')")`):
  - `POST /booking` — создание бронирования
  - `GET /booking/{id}` — получение по ID
  - `GET /bookings` — история пользователя
  - `DELETE /booking/{id}` — отмена бронирования

- **GlobalExceptionHandler**:
  - `ResourceNotFoundException` → 404
  - `RoomNotAvailableException` → 409
  - `AuthenticationException` → 401
  - `AccessDeniedException` → 403
  - `DuplicateKeyException` → 409
  - Generic `Exception` → 500

**Проверка**: `mvn spring-boot:run`, проверка всех эндпоинтов через curl.

---

## ЭТАП 14. API Gateway

**Цель**: Настроить маршрутизацию и передачу JWT.

### Что создаётся:
- **pom.xml**: Spring Cloud Gateway, Eureka Client, Spring Security
- **ApiGatewayApplication.java** — `@SpringBootApplication` + `@EnableDiscoveryClient`
- **application.yml**:
  - Порт: `8080`
  - Routes:
    - `/api/bookings/**` → `BookingService` (через Eureka)
    - `/api/hotels/**` → `HotelService` (через Eureka)
  - Discovery locator: `enabled=true`, `lowerCaseServiceId=true`
- **SecurityConfig** для Gateway:
  - Отключить CSRF
  - Разрешить доступ к `/eureka/**`, `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`
  - JWT-фильтр для проксируемых запросов: извлечь токен и передать в заголовке
- **GatewayFilter**:
  - Передача JWT в заголовке `Authorization` всем бэкенд-сервисам
  - Логируем каждый запрос (correlation ID)

**Проверка**: `mvn spring-boot:run`, проверка что запросы на `localhost:8080/api/hotels/**` проксируются в Hotel Service.

---

## ЭТАП 15. OpenAPI/Swagger документация

**Цель**: Подключить и настроить Swagger для всех сервисов.

### Что создаётся:
- Для каждого сервиса (Hotel, Booking):
  - Зависимость `springdoc-openapi-starter-webmvc-ui`
  - `@OpenAPIDefinition`, `@Info` в конфигурации
  - `@Operation`, `@ApiResponse` на контроллерах
  - Настройка пути Swagger UI: `/swagger-ui.html`
- Для Gateway: Swagger не требуется (нет собственных контроллеров), но можно добавить документацию маршрутов

**Проверка**: Открыть `http://localhost:8081/swagger-ui.html` (Hotel) и `http://localhost:8082/swagger-ui.html` (Booking).

---

## ЭТАП 16. Интеграционные тесты Booking Service

**Цель**: Протестировать полный цикл бронирования с моками Hotel Service.

### Что создаётся:
- **AuthControllerTest** — `@WebMvcTest`:
  - Регистрация нового пользователя
  - Вход и получение токена
  - Доступ без токена → 401
- **BookingControllerTest** — `@WebMvcTest` + моки:
  - Создание бронирования с `autoSelect=false` (указанroomId)
  - Создание бронирования с `autoSelect=true`
  - Получение истории бронирований
  - Отмена бронирования
  - Разные роли (USER vs ADMIN)
- **BookingServiceTest** — `@ExtendWith(MockitoExtension.class)`:
  - Успешный сценарий: PENDING → CONFIRMED
  - Сценарий ошибки: PENDING → CANCELLED (с компенсацией)
  - Сценарий таймаута: PENDING → CANCELLED
  - Идентичность запроса (дубликат не создаётся)
- **HotelServiceClientTest** — тест Feign-клиента с моками

**Проверка**: `mvn test` — все тесты зелёные.

---

## ЭТАП 17. Финальная сборка и проверка всей системы

**Цель**: Запустить все 4 сервиса и проверить полный цикл работы.

### Порядок запуска:
1. Eureka Server → `http://localhost:8761`
2. Hotel Management Service → `http://localhost:8081`
3. Booking Service → `http://localhost:8082`
4. API Gateway → `http://localhost:8080`

### Проверочные сценарии (curl):
1. **Регистрация пользователя**: `POST localhost:8080/api/user/register`
2. **Авторизация**: `POST localhost:8080/api/user/auth` → получить JWT
3. **Создание отеля** (ADMIN): `POST localhost:8080/api/hotels`
4. **Создание номера**: `POST localhost:8080/api/rooms`
5. **Получить номера**: `GET localhost:8080/api/rooms/recommend`
6. **Создать бронирование** (USER): `POST localhost:8080/api/booking` → PENDING → CONFIRMED
7. **История бронирований**: `GET localhost:8080/api/bookings`
8. **Отмена бронирования**: `DELETE localhost:8080/api/booking/{id}`

**Проверка**: Все шаги выполняются успешно, статусы бронирований корректно меняются.

---

## Сводная таблица эндпоинтов

| Сервис | Метод | Путь | Роль | Описание |
|--------|-------|------|------|----------|
| Booking | POST | /user/register | — | Регистрация |
| Booking | POST | /user/auth | — | Авторизация |
| Booking | POST | /user | ADMIN | Создать пользователя |
| Booking | PATCH | /user | ADMIN | Обновить пользователя |
| Booking | DELETE | /user | ADMIN | Удалить пользователя |
| Booking | POST | /booking | USER | Создать бронирование |
| Booking | GET | /booking/{id} | USER | Получить бронирование |
| Booking | GET | /bookings | USER | История бронирований |
| Booking | DELETE | /booking/{id} | USER | Отменить бронирование |
| Hotel | POST | /api/hotels | ADMIN | Создать отель |
| Hotel | POST | /api/rooms | ADMIN | Создать номер |
| Hotel | GET | /api/hotels | USER | Список отелей |
| Hotel | GET | /api/rooms | USER | Свободные номера |
| Hotel | GET | /api/rooms/recommend | USER | Рекомендуемые номера |
| Hotel | POST | /api/rooms/{id}/confirm-availability | INTERNAL | Подтвердить доступность |
| Hotel | POST | /api/rooms/{id}/release | INTERNAL | Снять блокировку |

---

## Итоговый стек технологий

| Компонент | Технология |
|-----------|-----------|
| Java | 17 |
| Фреймворк | Spring Boot 3.5.x |
| Cloud | Spring Cloud 2025.0.x (Levi) |
| Service Registry | Spring Cloud Eureka |
| API Gateway | Spring Cloud Gateway |
| REST | Spring Web MVC + Spring Cloud Gateway |
| ORM | Spring Data JPA + Hibernate |
| DB | H2 (in-memory, per service) |
| JWT | jjwt 0.12.x (jakarta) |
| Безопасность | Spring Security 6.x (Resource Server mode) |
| Клиент | Spring Cloud OpenFeign |
| Resilience | Resilience4j (retry, circuit breaker) |
| DTO | MapStruct |
| Boilerplate | Lombok |
| Docs | SpringDoc OpenAPI 2.x |
| Тесты | JUnit 5 + MockMvc + Mockito |
| Сборка | Maven |
