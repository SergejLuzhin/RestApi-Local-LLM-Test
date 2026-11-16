package com.skillfactory.luzhin.bookingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация SpringDoc OpenAPI для Booking Service.
 *
 * Настраивает метаданные API-документации (название, версия, описание, контакты)
 * и определение схемы безопасности JWT (Bearer Token, алгоритм HS256).
 *
 * Документация доступна по пути /swagger-ui.html.
 * JSON-спецификация — по пути /v3/api-docs.
 */
@Configuration
public class OpenAPIConfig {

    /**
     * Ключ схемы безопасности JWT для использования в аннотациях @SecurityRequirement.
     */
    private static final String JWT_SECURITY_SCHEME = "Bearer Token";

    /**
     * Создаёт объект OpenAPI с метаданными API и определением JWT-безопасности.
     *
     * @return настроенный объект OpenAPI для Booking Service
     */
    @Bean
    public OpenAPI bookingServiceOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME, jwtSecurityScheme()))
                .info(new Info()
                        .title("Booking Service API")
                        .description("REST API для управления бронированиями отелей. "
                                + "Включает регистрацию и авторизацию пользователей, "
                                + "двухшаговое бронирование с подтверждением доступности номеров "
                                + "через Hotel Management Service, а также административное управление пользователями.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Skillfactory — Luzhin Sergey")
                                .email("luzhin@skillfactory.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    /**
     * Определяет схему безопасности JWT Bearer Token (HS256).
     *
     * @return схема безопасности типа HTTP с токеном Bearer и алгоритмом HS256
     */
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT-токен, полученный через эндпоинт POST /user/auth. "
                        + "Токен включает userId, username и role (USER/ADMIN).");
    }
}
