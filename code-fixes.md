# Чек-лист исправлений кода (IDEA warnings/errors)

> Создан: 16 ноября 2026
> Источник инспекции: IntelliJ IDEA — 94 ошибки, 18 предупреждений

---

## Приоритет 1 — Обязательно (основная масса ошибок)

### Исправление 1: Заменить ручной getter/setter на Lombok в Booking Service DTO

**Файл:** `booking-service/.../dto/UserResponse.java`
**Было:** 154 строки (5 полей + 10 методов getter/setter + конструктор)
**Стало:** ~50 строк (5 полей + 4 аннотации Lombok)

**Действие:** Перезаписать файл полностью.

**Новое содержимое:**
```java
package com.skillfactory.luzhin.bookingservice.dto;

import com.skillfactory.luzhin.bookingservice.entity.UserRole;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для передачи информации о пользователе.
 *
 * Используется в ответах контроллеров для возврата данных о пользователе.
 * Пароль не включается в ответ из соображений безопасности.
 *
 * Поля:
 * - id — уникальный идентификатор пользователя;
 * - username — имя пользователя;
 * - role — роль пользователя (USER или ADMIN);
 * - createdAt — время создания записи;
 * - updatedAt — время последнего обновления.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /** Уникальный идентификатор пользователя. */
    private Long id;

    /** Имя пользователя для входа в систему. */
    private String username;

    /** Роль пользователя — определяет уровень доступа к функциям системы. */
    private UserRole role;

    /** Дата и время создания записи. */
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления записи. */
    private LocalDateTime updatedAt;
}
```

---

**Файл:** `booking-service/.../dto/BookingResponse.java`
**Было:** 234 строки (8 полей + 16 методов getter/setter + конструктор)
**Стало:** ~55 строк

**Действие:** Перезаписать файл полностью.

**Новое содержимое:**
```java
package com.skillfactory.luzhin.bookingservice.dto;

import com.skillfactory.luzhin.bookingservice.entity.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для передачи информации о бронировании.
 *
 * Используется в ответах контроллеров для возврата данных о бронировании.
 * Содержит полную информацию о бронировании, включая данные о пользователе.
 *
 * Поля:
 * - id — уникальный идентификатор бронирования;
 * - userId — идентификатор пользователя, создавшего бронирование;
 * - username — имя пользователя, создавшего бронирование;
 * - roomId — идентификатор номера;
 * - startDate — дата и время начала бронирования;
 * - endDate — дата и время окончания бронирования;
 * - status — текущий статус бронирования;
 * - createdAt — время создания записи.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    /** Уникальный идентификатор бронирования. */
    private Long id;

    /** Идентификатор пользователя, создавшего бронирование. */
    private Long userId;

    /** Имя пользователя, создавшего бронирование. */
    private String username;

    /** Идентификатор номера в отеле. */
    private Long roomId;

    /** Дата и время начала бронирования. */
    private LocalDateTime startDate;

    /** Дата и время окончания бронирования. */
    private LocalDateTime endDate;

    /** Текущий статус бронирования. PENDING, CONFIRMED или CANCELLED. */
    private BookingStatus status;

    /** Дата и время создания записи. */
    private LocalDateTime createdAt;
}
```

---

**Файл:** `booking-service/.../dto/BookingCreateRequest.java`
**Было:** 124 строки (4 поля + 8 методов getter/setter)
**Стало:** ~55 строк

**Действие:** Перезаписать файл полностью.

**Новое содержимое:**
```java
package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO (Transfer Object) для запроса на создание нового бронирования.
 *
 * Используется в эндпоинте POST /booking.
 *
 * Поля:
 * - roomId — идентификатор номера (обязательный, если autoSelect = false);
 * - startDate — дата и время начала бронирования (обязательная, будущая);
 * - endDate — дата и время окончания бронирования (обязательная, будущая, позже startDate);
 * - autoSelect — флаг автоподбора номера (true — номер подбирается автоматически
 *                на основе алгоритма равномерной загрузки, false — используется указанный roomId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    /** Идентификатор номера. Обязателен, если autoSelect = false. Игнорируется при autoSelect = true. */
    @NotNull(message = "Идентификатор номера не может быть null")
    private Long roomId;

    /** Дата и время начала бронирования. Должна быть в будущем. */
    @NotNull(message = "Дата начала не может быть null")
    @Future(message = "Дата начала должна быть в будущем")
    private LocalDateTime startDate;

    /** Дата и время окончания бронирования. Должна быть в будущем и позже даты начала. */
    @NotNull(message = "Дата окончания не может быть null")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDateTime endDate;

    /** Флаг автоматического подбора номера. true — автоподбор; false — использовать roomId. */
    @Builder.Default
    private boolean autoSelect = false;
}
```

---

**Файл:** `booking-service/.../dto/UserRegisterRequest.java`
**Было:** 68 строк (2 поля + 4 метода getter/setter)
**Стало:** ~40 строк

**Действие:** Перезаписать файл полностью.

**Новое содержимое:**
```java
package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO (Transfer Object) для запроса на регистрацию нового пользователя.
 *
 * Используется в эндпоинте POST /user/register.
 *
 * Поля:
 * - username — уникальное имя пользователя (обязательное, 3-50 символов);
 * - password — пароль для входа (обязательный, минимум 6 символов).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterRequest {

    /** Уникальное имя пользователя для входа в систему. От 3 до 50 символов. */
    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
    private String username;

    /** Пароль для входа в систему. Минимум 6 символов. */
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;
}
```

---

**Файл:** `booking-service/.../dto/UserLoginRequest.java`
**Было:** 63 строки (2 поля + 4 метода getter/setter)
**Стало:** ~40 строк

**Действие:** Перезаписать файл полностью.

**Новое содержимое:**
```java
package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO (Transfer Object) для запроса на авторизацию пользователя.
 *
 * Используется в эндпоинте POST /user/auth.
 *
 * Поля:
 * - username — имя пользователя для входа;
 * - password — пароль для проверки аутентификации.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginRequest {

    /** Имя пользователя для входа в систему. */
    @NotBlank(message = "Имя пользователя не может быть пустым")
    private String username;

    /** Пароль для проверки аутентификации. */
    @NotBlank(message = "Пароль не может быть пустым")
    private String password;
}
```

---

**Файл:** `booking-service/.../dto/UserUpdateRequest.java`
**Было:** 66 строк (2 поля + 4 метода getter/setter)
**Стало:** ~40 строк

**Действие:** Перезаписать файл полностью.

**Новое содержимое:**
```java
package com.skillfactory.luzhin.bookingservice.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO (Transfer Object) для запроса на обновление данных пользователя.
 *
 * Используется в эндпоинте PATCH /user.
 * Все поля опциональны — обновляются только переданные значения.
 *
 * Поля:
 * - username — новое имя пользователя (опционально, 3-50 символов);
 * - password — новый пароль (опционально, минимум 6 символов).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    /** Новое имя пользователя (опционально). От 3 до 50 символов, если передано. */
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
    private String username;

    /** Новый пароль (опционально). Минимум 6 символов, если передан. */
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;
}
```

---

### Исправление 2: Удалить неиспользуемый import в HotelControllerTest

**Файл:** `hotel-service/.../controller/HotelControllerTest.java`

**Строка 12 — удалить:**
```java
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
```

**Почему:** Аннотация `@AutoConfigureMockMvc` не используется в тесте. Тесты применяют ручную настройку MockMvc через `MockMvcBuilders.webAppContextSetup()`.

---

### Исправление 3: Исправить опечатку Cyrillic в RoomController

**Файл:** `hotel-service/.../controller/RoomController.java`

**Строка 62:**
```java
// Было (Cyrillic 'о' в 'operacionno'):
description = "Возвращает все номера с operacionno доступностью. Не учитывает..."
// Стало (Latin 'o' в 'operational'):
description = "Возвращает все номера, которые операционно доступны. Не учитывает..."
```

---

## Приоритет 2 — Рекомендуется

### Исправление 4: Заменить `Boolean` на `boolean` в RoomCreateRequest

**Файл:** `hotel-service/.../dto/RoomCreateRequest.java`

**Строки 37-38:**
```java
// Было:
@Builder.Default
private Boolean available = true;

// Стало:
private boolean available = true;
```

**Почему:** `@Builder.Default` на wrapper-типе `Boolean` может вызвать предупреждение IDE о возможном `NullPointerException`. Primitive `boolean` безопаснее. Lombok-билдер автоматически установит `false`, если поле не передано.

---

### Исправление 5: Добавить импорт `@PrePersist` и `@PreUpdate` в BaseEntity

**Файл:** `hotel-service/.../entity/BaseEntity.java`

**Добавить в импорты (строка 4):**
```java
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
```

**Заменить строки 33 и 42:**
```java
// Было (полностью квалифицированные аннотации):
@jakarta.persistence.PrePersist
@jakarta.persistence.PreUpdate

// Стало:
@PrePersist
@PreUpdate
```

---

### Исправление 6: Устранить конфликт Lombok `@Data` + `@PreUpdate` в Booking Service entities

**Файлы:** `User.java`, `Booking.java`

**Проблема:** Lombok `@Data` генерирует `setUpdatedAt()`, а `@PreUpdate` определяет `onUpdate()`. IntelliJ предупреждает, что Lombok-генерируемый setter может быть конфликтным.

**Решение для `User.java`:**

Заменить:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

На:
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

Удалить методы `onCreate()` и `onUpdate()` (строки 78-94) — они не нужны, так как `createdAt` и `updatedAt` инициализируются автоматически.

**Но!** Это изменит поведение. Если `@PrePersist`/`@PreUpdate` нужны для автозаполнения дат, то оставить как есть, но добавить suppress-аннотации для подавления предупреждений IDE:

```java
@SuppressWarnings("squid:S1192") // Подавление конфликта Lombok setter с @PreUpdate
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
}

@PrePersist
@PreUpdate
protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

**Или более чистое решение** — использовать `@Builder` с `@SuperBuilder` из Lombok, который корректно работает с JPA lifecycle callback.

---

## Сводная таблица всех изменений

| # | Файл | Тип изменения | Строк до | Строк после | Приоритет |
|---|------|--------------|----------|-------------|-----------|
| 1 | `UserResponse.java` | Lombok вместо ручных методов | 154 | ~50 | 🔴 |
| 2 | `BookingResponse.java` | Lombok вместо ручных методов | 234 | ~55 | 🔴 |
| 3 | `BookingCreateRequest.java` | Lombok вместо ручных методов | 124 | ~55 | 🔴 |
| 4 | `UserRegisterRequest.java` | Lombok вместо ручных методов | 68 | ~40 | 🔴 |
| 5 | `UserLoginRequest.java` | Lombok вместо ручных методов | 63 | ~40 | 🔴 |
| 6 | `UserUpdateRequest.java` | Lombok вместо ручных методов | 66 | ~40 | 🔴 |
| 7 | `HotelControllerTest.java` | Удалить unused import | 315 | 314 | 🟡 |
| 8 | `RoomController.java` | Исправить опечатку | 141 | 141 | 🟡 |
| 9 | `RoomCreateRequest.java` | `Boolean` → `boolean` | 39 | 39 | 🟢 |
| 10 | `BaseEntity.java` | Добавить импорты | 46 | 48 | 🟢 |
| 11 | `User.java` | Устранить конфликт Lombok + JPA | 95 | ~95 | 🟢 |
| 12 | `Booking.java` | Устранить конфликт Lombok + JPA | 119 | ~119 | 🟢 |

**Итого экономия кода:** ~780 строк → ~280 строк (экономия ~500 строк boilerplate).
