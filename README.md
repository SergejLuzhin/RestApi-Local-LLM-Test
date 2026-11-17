# 🏨 Hotel Booking System

Микросервисная система бронирования номеров отелей, реализованная на **Spring Boot** с использованием **Spring Cloud**.

Проект демонстрирует распределённую архитектуру с сервис-реестром, API-шлюзом, JWT-аутентификацией, двухфазными транзакциями и алгоритмом равномерной загрузки номеров.

---

## 📋 Содержание

- [Архитектура](#архитектура)
- [Технологический стек](#технологический-стек)
- [API-эндпоинты](#api-эндпоинты)
  - [Аутентификация](#аутентификация)
  - [Управление отелями](#управление-отелями)
  - [Управление номерами](#управление-номерами)
  - [Бронирование](#бронирование)
- [Безопасность](#безопасность)
- [Двухфазная транзакция бронирования](#двухфазная-транзакция-бронирования)
- [Автоматические проверки](#автоматические-проверки)
- [Запуск проекта](#запуск-проекта)
- [Проверка работы](#проверка-работы)
- [Структура проекта](#структура-проекта)
- [Дополнительно](#дополнительно)

---

## Архитектура

Проект состоит из **4 микросервисов**, объединённых через **Spring Cloud Gateway**:

```
                    ┌─────────────────┐
                    │  API Gateway    │
                    │   (порт 8080)   │
                    └────────┬────────┘
                             │ RewritePath /api/... → /...
              ┌──────────────┴──────────────┐
              │                             │
    ┌─────────▼─────────┐         ┌─────────▼─────────┐
    │ Hotel Management  │         │     Booking        │
    │   Service         │         │     Service        │
    │   (порт 8081)     │         │     (порт 8082)    │
    └───────────────────┘         └───────────────────┘
              │                             │
              └──────────┬──────────────────┘
                         │ Eureka Service Registry
                   ┌─────▼─────┐
                   │  Eureka   │
                   │  (8761)   │
                   └───────────┘
```

### Сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| **Eureka Server** | 8761 | Реестр сервисов (Service Registry) |
| **API Gateway** | 8080 | Единая точка входа, маршрутизация, переписывание путей |
| **Hotel Management Service** | 8081 | CRUD отелей и номеров, проверка доступности, временная блокировка |
| **Booking Service** | 8082 | Регистрация, аутентификация, создание и управление бронированиями |

---

## Технологический стек

| Категория | Технология | Версия |
|-----------|-----------|--------|
| **Фреймворк** | Spring Boot | 3.5.3 |
| **Cloud** | Spring Cloud | 2025.0.0 (Levi) |
| **Java** | OpenJDK 25 (bytecode 17) | — |
| **JWT** | jjwt | 0.12.6 |
| **Реестр** | Eureka Server | — |
| **API Gateway** | Spring Cloud Gateway + LoadBalancer | — |
| **БД** | H2 (in-memory) | — |
| **ORM** | Spring Data JPA | — |
| **Документация** | SpringDoc OpenAPI (Swagger) | 2.8.9 |
| **Резильентность** | Resilience4j (Circuit Breaker, Retry) | 2.2.0 |
| **Картирование DTO** | MapStruct | 1.6.3 |
| **Утилиты** | Lombok, MapStruct Binding | 1.18.48 / 1.6.3 |

---

## API-эндпоинты

Все запросы идут через **API Gateway** на порту **8080** с префиксом `/api/`.

### Аутентификация

| Метод | Эндпоинт | Роль | Описание |
|-------|----------|------|----------|
| `POST` | `/api/user/register` | Публичный | Регистрация нового пользователя (роль USER по умолчанию) |
| `POST` | `/api/user/auth` | Публичный | Авторизация по username/password, возврат JWT-токена |

**Тело регистрации:**
```json
{ "username": "john", "password": "secret123" }
```

**Тело авторизации:**
```json
{ "username": "john", "password": "secret123" }
```

**Ответ:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "username": "john",
  "role": "ROLE_USER"
}
```

### Управление отелями

| Метод | Эндпоинт | Роль | Описание |
|-------|----------|------|----------|
| `POST` | `/api/hotels` | ADMIN | Создать отель |
| `GET` | `/api/hotels` | USER, ADMIN | Получить список всех отелей |
| `GET` | `/api/hotels/{id}` | USER, ADMIN | Получить отель по ID |
| `PATCH` | `/api/hotels/{id}` | ADMIN | Обновить отель |
| `DELETE` | `/api/hotels/{id}` | ADMIN | Удалить отель (каскадно с номерами) |

### Управление номерами

| Метод | Эндпоинт | Роль | Описание |
|-------|----------|------|----------|
| `POST` | `/api/rooms` | ADMIN | Создать номер в отеле |
| `GET` | `/api/rooms` | USER, ADMIN | Получить все доступные номера |
| `GET` | `/api/rooms/recommend` | USER, ADMIN | Рекомендованные номера (по алгоритму равномерной загрузки) |
| `POST` | `/api/rooms/{id}/confirm-availability` | Внутренний | Подтвердить доступность + заблокировать слот |
| `POST` | `/api/rooms/{id}/release` | Внутренний | Снять временную блокировку (компенсация) |

### Бронирование

| Метод | Эндпоинт | Роль | Описание |
|-------|----------|------|----------|
| `POST` | `/api/bookings` | USER | Создать бронирование |
| `GET` | `/api/bookings/{id}` | USER | Получить бронирование по ID (только своё) |
| `GET` | `/api/bookings/bookings` | USER | Получить историю бронирований |
| `DELETE` | `/api/bookings/{id}` | USER | Отменить бронирование |

**Тело создания бронирования:**
```json
{
  "roomId": 1,
  "startDate": "2026-12-01T14:00:00",
  "endDate": "2026-12-05T11:00:00",
  "autoSelect": false
}
```

| Поле | Тип | Описание |
|------|-----|----------|
| `roomId` | Long | Идентификатор номера (обязателен при `autoSelect=false`) |
| `startDate` | LocalDateTime | Дата и время заезда (должна быть в будущем) |
| `endDate` | LocalDateTime | Дата и время выезда (позже startDate) |
| `autoSelect` | Boolean | `true` — автоподбор наименее загруженного номера |

---

## Безопасность

### JWT-аутентификация

- **Алгоритм:** HS256 (HMAC with SHA-256)
- **Срок действия токена:** 1 час (конфигурируется в `jwt.expiration-ms`)
- **Передача:** заголовок `Authorization: Bearer <token>`
- **Секретный ключ:** Base64-закодированный, общий для всех сервисов

### Ролевая модель

| Роль | Доступ |
|------|--------|
| **USER** | Регистрация, авторизация, управление бронированиями, просмотр отелей и номеров |
| **ADMIN** | Всё, что у USER + CRUD отелей и номеров |

### Механизмы защиты

- **BCrypt** для хеширования паролей
- **`@PreAuthorize`** для разграничения доступа по ролям на уровне контроллеров
- **Stateless-сессии** — сервер не хранит состояние
- **AuthenticationEntryPoint** — возврат HTTP 401 для неавторизованных запросов
- **AccessDeniedHandler** — возврат HTTP 403/401 при недостатке прав
- **CSRF отключён** — не требуется для stateless JWT

### Администратор по умолчанию

При старте Booking Service создаётся администратор:
- **username:** `admin`
- **password:** `password123`

---

## Двухфазная транзакция бронирования

Создание бронирования — атомарная операция между двумя сервисами:

```
1. PENDING     — бронирование сохранено локально (Booking Service)
2. CONFIRMED   — Hotel Service подтвердил доступность
3. CANCELLED   — Hotel Service отказал; слот освобождён (компенсация)
```

### Алгоритм

```
POST /bookings
  │
  ├─► 1. Создать Booking(status=PENDING) ──────────────────► Booking DB
  │
  ├─► 2. Вызвать Hotel Service: POST /rooms/{id}/confirm-availability
  │        │
  │        ├─► OK   → Booking(status=CONFIRMED)     ✅
  │        │
  │        └─► FAIL → Booking(status=CANCELLED)
  │                     └─► POST /rooms/{id}/release  (компенсация)
  │
  └─► Ответ клиенту
```

### Резильентность

- **Retry** — до 3 попыток с экспоненциальной задержкой (500ms × 2, max 5000ms)
- **Circuit Breaker** — при частых отказах Hotel Service вызовы прерываются
- **Correlation ID** — сквозной UUID для трассировки всех шагов транзакции

---

## Автоматические проверки

В проекте реализовано **67 автоматических тестов**, охватывающих все критические сценарии.

### Hotel Management Service — 42 теста

| Модуль | Кол-во | Что тестируется |
|--------|--------|-----------------|
| `HotelControllerTest` | 10 | CRUD отелей через REST (201, 404, 403) |
| `HotelRepositoryTest` | 8 | CRUD отелей через репозиторий, каскадное удаление номеров |
| `RoomControllerTest` | 10 | CRUD номеров, проверка доступности, снятие блокировки |
| `HotelServiceTest` | 6 | Бизнес-логика: создание, обновление, удаление отелей |
| `RoomServiceTest` | 8 | Создание, удаление номеров, блокировка/разблокировка слотов |

### Booking Service — 25 тестов

| Модуль | Кол-во | Что тестируется |
|--------|--------|-----------------|
| `BookingControllerTest` | 8 | Создание/просмотр/отмена бронирований, доступ без токена |
| `AuthControllerTest` | 7 | Регистрация, авторизация, валидация данных |
| `BookingServiceTest` | 6 | Двухфазная транзакция, автоподбор, компенсации |
| `HotelServiceClientTest` | 4 | Feign-клиент: подтверждение, освобождение слотов, рекомендации |

### Запуск тестов

```bash
# Все тесты
mvn clean test

# Только Hotel Service
mvn clean test -pl hotel-service

# Только Booking Service
mvn clean test -pl booking-service
```

---

## Запуск проекта

### Требования

- **JDK 17+** (рекомендуется OpenJDK 25)
- **Maven 3.9+**
- **Git**
- **IntelliJ IDEA** (рекомендуется)

### Сборка проекта

```bash
# 1. Клонировать репозиторий
git clone <repository-url>
cd RestApi-Local-LLM-Test

# 2. Собрать проект (без запуска тестов)
mvn clean package -DskipTests
```

### Запуск сервисов

Все сервисы запускаются через Maven. Рекомендуется открывать каждый сервис в **отдельном окне терминала** или **отдельном профиле запуска в IntelliJ IDEA**.

#### 1. Eureka Server — реестр сервисов

```bash
cd eureka-server
mvn spring-boot:run
```

- **Порт:** 8761
- **Eureka Dashboard:** http://localhost:8761
- **Swagger:** не используется (сервис реестра)

> Запустите первым. Дождитесь в логах `Started EurekaServerApplication`.

#### 2. Hotel Management Service — управление отелями и номерами

```bash
cd hotel-service
mvn spring-boot:run
```

- **Порт:** 8081
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8081/v3/api-docs
- **H2 Console:** http://localhost:8081/h2-console (пароль: `sa`, драйвер: `org.h2.Driver`)

> Запустите после Eureka. Дождитесь `Started HotelServiceApplication`. Сервис автоматически зарегистрируется в Eureka.

#### 3. Booking Service — аутентификация и бронирования

```bash
cd booking-service
mvn spring-boot:run
```

- **Порт:** 8082
- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8082/v3/api-docs
- **H2 Console:** http://localhost:8082/h2-console (пароль: `sa`, драйвер: `org.h2.Driver`)

> Запустите после Eureka. Дождитесь `Started BookingServiceApplication`. Сервис автоматически зарегистрируется в Eureka.

**Администратор по умолчанию** создаётся автоматически при старте Booking Service (через `data.sql`):

| Поле | Значение |
|------|----------|
| **username** | `admin` |
| **password** | `password123` |
| **role** | `ROLE_ADMIN` |

> Администратор нужен для создания отелей и номеров. Для бронирований подходит обычная регистрация пользователя.

#### 4. API Gateway — единая точка входа

```bash
cd api-gateway
mvn spring-boot:run
```

- **Порт:** 8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Swagger Resources:** http://localhost:8080/swagger-resources

> Запустите последним. Gateway переписывает все запросы с префиксом `/api/` во внутренние пути сервисов.

### Запуск из IntelliJ IDEA

1. Откройте корень проекта в IntelliJ IDEA
2. Убедитесь, что Maven-проекты распознаны (`Maven` → Reload All Maven Projects)
3. В каждом модуле найдите файл `*Application.java` и нажмите ▶ (Run)
4. **Порядок запуска:** `EurekaServerApplication` → `HotelServiceApplication` → `BookingServiceApplication` → `ApiGatewayApplication`
5. Убедитесь, что каждый сервис успешно стартовал (в логах `Started ...Application`) до запуска следующего

### Проверка запуска

Откройте http://localhost:8761 — на Eureka Dashboard должны отобразиться зарегистрированные сервисы:

- `HOTEL-SERVICE`
- `BOOKING-SERVICE`
- `API-GATEWAY`

---

## Проверка работы

Полный сценарий проверки системы через API Gateway (`http://localhost:8080`):

### 1. Регистрация пользователя

```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

**Ответ:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "username": "testuser",
  "role": "ROLE_USER"
}
```

### 2. Авторизация

```bash
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

Сохраните полученный `token`.

### 3. Создание отеля (admin)

```bash
# Авторизуйтесь как admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' | jq -r '.token')

# Создайте отель
curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Grand Hotel","address":"Moscow, Tverskaya 1"}'
```

**Ответ:**
```json
{ "id": 1, "name": "Grand Hotel", "address": "Moscow, Tverskaya 1" }
```

### 4. Создание номера

```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"hotelId":1,"number":"101","available":true}'
```

### 5. Получение рекомендованных номеров

```bash
curl http://localhost:8080/api/rooms/recommend \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6. Создание бронирования

```bash
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' | jq -r '.token')

curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 1,
    "startDate": "2026-12-01T14:00:00",
    "endDate": "2026-12-05T11:00:00",
    "autoSelect": false
  }'
```

**Ответ:**
```json
{
  "id": 1,
  "userId": 1,
  "username": "testuser",
  "roomId": 1,
  "startDate": "2026-12-01T14:00:00",
  "endDate": "2026-12-05T11:00:00",
  "status": "CONFIRMED",
  "createdAt": "2026-11-17T12:00:00"
}
```

### 7. Автоподбор номера (`autoSelect: true`)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": null,
    "startDate": "2026-12-10T14:00:00",
    "endDate": "2026-12-15T11:00:00",
    "autoSelect": true
  }'
```

### 8. Получение истории бронирований

```bash
curl http://localhost:8080/api/bookings/bookings \
  -H "Authorization: Bearer $USER_TOKEN"
```

### 9. Получение конкретного бронирования

```bash
curl http://localhost:8080/api/bookings/1 \
  -H "Authorization: Bearer $USER_TOKEN"
```

### 10. Отмена бронирования

```bash
curl -X DELETE http://localhost:8080/api/bookings/1 \
  -H "Authorization: Bearer $USER_TOKEN"
```

**Ответ:** `204 No Content`

### Проверка через Swagger

Каждый сервис имеет встроенную Swagger-документацию:

| Сервис | URL |
|--------|-----|
| Hotel Management | http://localhost:8081/swagger-ui.html |
| Booking | http://localhost:8082/swagger-ui.html |

---

## Структура проекта

```
hotel-booking-system/
├── eureka-server/              # Сервис реестра
│   └── src/main/java/...       # EurekaApplication
├── api-gateway/                # API Gateway + маршрутизация
│   └── src/main/               # application.yml, routes
├── hotel-service/              # Управление отелями и номерами
│   └── src/main/java/com/skillfactory/luzhin/hotelservice/
│       ├── entity/             # Hotel, Room, BaseEntity
│       ├── repository/         # JPA-репозитории
│       ├── service/            # Бизнес-логика
│       ├── controller/         # REST-контроллеры
│       ├── dto/                # DTO для запросов/ответов
│       ├── config/             # SecurityConfig, JwtUtil
│       └── exception/          # GlobalExceptionHandler
├── booking-service/            # Аутентификация + бронирования
│   └── src/main/java/com/skillfactory/luzhin/bookingservice/
│       ├── entity/             # User, Booking, BaseEntity
│       ├── repository/         # JPA-репозитории
│       ├── service/            # AuthService, BookingService
│       ├── controller/         # AuthController, BookingController
│       ├── dto/                # DTO для запросов/ответов
│       ├── client/             # Feign-клиент → Hotel Service
│       ├── config/             # SecurityConfig, JwtUtil, JwtFilter
│       └── exception/          # GlobalExceptionHandler
└── pom.xml                     # Родительский POM
```

---

## Дополнительно

### OpenAPI / Swagger

Документация API автоматически генерируется SpringDoc:

- **Hotel Service:** http://localhost:8081/swagger-ui.html
- **Booking Service:** http://localhost:8082/swagger-ui.html

### Конфигурация

Все сервисы используют YAML-конфигурацию в `src/main/resources/application.yml`:

| Параметр | Описание | Значение по умолчанию |
|----------|----------|----------------------|
| `jwt.secret` | Base64-секрет для JWT | `YWJjZGVm...` (shared) |
| `jwt.expiration-ms` | TTL токена в мс | `3600000` (1 час) |
| `spring.data.sql.init.mode` | Инициализация БД | `always` (deferred) |
| `eureka.client.service-url` | URL Eureka Server | `http://localhost:8761/eureka` |

### Алгоритм равномерной загрузки

При `autoSelect=true` Booking Service запрашивает у Hotel Service список номеров,
отсортированный по `timesBooked ASC, id ASC`. Выбирается номер с наименьшим
количеством бронирований — это обеспечивает равномерное распределение загрузки
между номерами.
