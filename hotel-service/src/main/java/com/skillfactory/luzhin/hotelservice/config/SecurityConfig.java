package com.skillfactory.luzhin.hotelservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация безопасности для Hotel Service с JWT-аутентификацией.
 * Использует stateless-режим с проверкой JWT-токенов для каждого запроса.
 *
 * <p>Особенности конфигурации:
 * <ul>
 *   <li>Stateless-сессия — токены передаются в заголовке Authorization, сервер не хранит сессию</li>
 *   <li>JWT-фильтрация — JwtAuthenticationFilter извлекает и валидирует токен из заголовка</li>
 *   <li>Метод безопасности — @PreAuthorize аннотации на контроллерах для разграничения ролей</li>
 *   <li>Публичные эндпоинты — Swagger/OpenAPI документация и actuator доступны без аутентификации</li>
 *   <li>AuthenticationEntryPoint — возврат HTTP 401 для неавторизованных запросов</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see JwtUtil
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Фильтр аутентификации JWT.
     * Вставляется перед стандартным фильтром UsernamePasswordAuthenticationFilter
     * для перехвата и обработки Bearer-токенов.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Точка входа для обработки ошибок аутентификации.
     * Возвращает HTTP 401 Unauthorized вместо 500 Internal Server Error
     * при попытке доступа к защищённым ресурсам без валидного JWT-токена.
     *
     * @param request     HTTP-запрос
     * @param response    HTTP-ответ
     * @param authException исключение аутентификации
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"timestamp\":\"" + java.time.Instant.now()
                    + "\",\"status\":401,\"error\":\"Unauthorized\","
                    + "\"message\":\"" + authException.getMessage() + "\"}");
        };
    }

    /**
     * Настройка цепочки фильтров безопасности с JWT-аутентификацией.
     *
     * <p>Правила авторизации:
     * <ul>
     *   <li>Swagger/OpenAPI ресурсы — доступ без аутентификации</li>
     *   <li>GET /api/hotels, GET /api/rooms, GET /api/rooms/recommend — публичный доступ</li>
     *   <li>POST /api/rooms/{id}/confirm-availability, POST /api/rooms/{id}/release — внутренний доступ</li>
     *   <li>Все остальные запросы — требуют аутентификации</li>
     * </ul>
     *
     * @param http объект HttpSecurity для настройки
     * @param entryPoint точка входа для обработки ошибок аутентификации
     * @return настроенная SecurityFilterChain
     * @throws Exception ошибка при настройке безопасности
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationEntryPoint entryPoint) throws Exception {
        http
                // Отключаем CSRF, так как используем stateless JWT-токены
                .csrf(AbstractHttpConfigurer::disable)

                // Настраиваем stateless-режим — сервер не создаёт HTTP-сессию
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Настраиваем правила авторизации для различных URL-паттернов
                // Для Spring Security 6.x с PathPatternParser используем «**» только
                // в конце паттерна (без последующих сегментов пути)
                .authorizeHttpRequests(auth -> auth
                        // Публичный доступ к Swagger/OpenAPI документации
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/actuator/**",
                                "/h2-console/**"
                        ).permitAll()

                        // Публичный доступ к GET-эндпоинтам для поиска отелей и номеров
                        .requestMatchers("/api/hotels").permitAll()
                        .requestMatchers("/api/hotels/**").permitAll()
                        .requestMatchers("/api/rooms").permitAll()
                        .requestMatchers("/api/rooms/recommend").permitAll()

                        // Внутренние эндпоинты подтверждения доступности и снятия блокировки
                        .requestMatchers("/api/rooms/{roomId}/confirm-availability").permitAll()
                        .requestMatchers("/api/rooms/{roomId}/release").permitAll()

                        // Все остальные запросы к API требуют аутентификации
                        .requestMatchers("/api/**").authenticated()

                        // Остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )

                // Настраиваем обработку ошибок аутентификации и авторизации
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(entryPoint)
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                    response.getWriter().write(
                                            "{\"timestamp\":\"" + java.time.Instant.now()
                                            + "\",\"status\":401,\"error\":\"Unauthorized\","
                                            + "\"message\":\"Authentication required\"}");
                                }))

                // Добавляем JWT-фильтр перед стандартным фильтром аутентификации
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Разрешаем H2 Console фрейм-опции для работы в iframe
        http.headers(headers ->
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}
