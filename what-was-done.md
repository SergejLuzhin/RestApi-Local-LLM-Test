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

### Следующий этап
**Этап 4: Hotel Service — сущности и репозитории**
