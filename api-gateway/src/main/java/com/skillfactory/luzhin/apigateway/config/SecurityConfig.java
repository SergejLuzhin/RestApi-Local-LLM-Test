package com.skillfactory.luzhin.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Конфигурация безопасности API Gateway для Spring WebFlux.
 *
 * API Gateway выполняет роль маршрутизатора и не валидирует JWT-токены самостоятельно.
 * Валидацию JWT выполняют бэкенд-сервисы (Booking Service и Hotel Service) как Resource Server.
 * Gateway перенаправляет входящий заголовок Authorization (Bearer-токен) без изменений
 * к соответствующим микросервисам через механизм маршрутизации Spring Cloud Gateway.
 *
 * Правила доступа:
 * - Публичные эндпоинты: /eureka/**, /actuator/** — доступны без аутентификации;
 * - Swagger-документация: /swagger-ui/**, /v3/api-docs/** — доступна без аутентификации;
 * - Все остальные запросы: проксируются к бэкенд-сервисам, где выполняется проверка JWT.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Конфигурация цепочки фильтров безопасности для WebFlux.
     * Отключает CSRF и устанавливает Stateless-режим без хранения сессии.
     * Разрешает доступ к публичным эндпоинтам (Eureka, Actuator, Swagger).
     * Все остальные запросы проксируются к бэкенд-сервисам.
     *
     * @param http объект ServerHttpSecurity для конфигурации
     * @return настроенная цепочка фильтров безопасности
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // CSRF не требуется для REST API
            .authorizeExchange(exchange -> exchange
                // Eureka Server — публичный доступ для регистрации сервисов
                .pathMatchers("/eureka/**").permitAll()
                // Actuator — публичный доступ для мониторинга
                .pathMatchers("/actuator/**").permitAll()
                // Swagger UI — публичный доступ к документации
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/swagger-ui.html").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                .pathMatchers("/swagger-resources/**").permitAll()
                // Все остальные запросы — доступны для проксирования к бэкенд-сервисам
                .anyExchange().permitAll()
            );

        return http.build();
    }
}
