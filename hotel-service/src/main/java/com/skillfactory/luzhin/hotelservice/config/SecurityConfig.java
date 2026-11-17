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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
@SuppressWarnings("unused") // поля注入руются Spring, методы вызываются Spring Security
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
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return this::unauthorizedResponse;
    }

    /**
     * Формирует ответ 401 Unauthorized при попытке доступа к защищённым ресурсам.
     */
    private void unauthorizedResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":401,\"error\":\"Unauthorized\","
                + "\"message\":\"" + authException.getMessage() + "\"}");
    }

    /**
     * Формирует ответ 401 Unauthorized при недостатке прав доступа.
     */
    private void accessDeniedResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":401,\"error\":\"Unauthorized\","
                + "\"message\":\"Authentication required\"}");
    }

    /**
     * Настройка цепочки фильтров безопасности с JWT-аутентификацией.
     *
     * <p>Правила авторизации:
     * <ul>
     *   <li>Swagger/OpenAPI ресурсы — доступ без аутентификации</li>
     *   <li>GET /hotels, GET /rooms, GET /rooms/recommend — публичный доступ</li>
     *   <li>POST /rooms/{id}/confirm-availability, POST /rooms/{id}/release — внутренний доступ</li>
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
                        .requestMatchers("/hotels").permitAll()
                        .requestMatchers("/hotels/**").permitAll()
                        .requestMatchers("/rooms").permitAll()
                        .requestMatchers("/rooms/recommend").permitAll()

                        // Внутренние эндпоинты подтверждения доступности и снятия блокировки
                        .requestMatchers("/rooms/{roomId}/confirm-availability").permitAll()
                        .requestMatchers("/rooms/{roomId}/release").permitAll()

                        // Все остальные запросы к API требуют аутентификации
                        .requestMatchers("/**").authenticated()

                        // Остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )

                // Настраиваем обработку ошибок аутентификации и авторизации
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(entryPoint)
                                .accessDeniedHandler(this::accessDeniedResponse))

                // Добавляем JWT-фильтр перед стандартным фильтром аутентификации
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Разрешаем H2 Console фрейм-опции для работы в iframe
        http.headers(headers ->
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}
