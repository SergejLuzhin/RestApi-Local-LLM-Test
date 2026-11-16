package com.skillfactory.luzhin.bookingservice.config;

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
 * Конфигурация безопасности для Booking Service с JWT-аутентификацией.
 * Использует stateless-режим с проверкой JWT-токенов для каждого запроса.
 *
 * <p>Особенности конфигурации:
 * <ul>
 *   <li>Stateless-сессия — токены передаются в заголовке Authorization, сервер не хранит сессию</li>
 *   <li>JWT-фильтрация — {@link JwtAuthenticationFilter} извлекает и валидирует токен из заголовка</li>
 *   <li>Метод безопасности — {@code @PreAuthorize} аннотации на контроллерах для разграничения ролей</li>
 *   <li>Публичные эндпоинты — регистрация, авторизация, Swagger/OpenAPI доступны без аутентификации</li>
 *   <li>Защищённые эндпоинты — все остальные операции требуют валидного JWT-токена</li>
 * </ul>
 *
 * <p>Правила авторизации:
 * <ul>
 *   <li>POST /user/register — без аутентификации (регистрация нового пользователя)</li>
 *   <li>POST /user/auth — без аутентификации (авторизация и получение JWT)</li>
 *   <li>Swagger/OpenAPI ресурсы — без аутентификации</li>
 *   <li>Все остальные эндпоинты — требуют аутентификации</li>
 *   <li>Разграничение ролей (USER/ADMIN) реализуется через {@code @PreAuthorize} на контроллерах</li>
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
     * <p>Правила авторизации для Booking Service:
     * <ul>
     *   <li>Swagger/OpenAPI ресурсы — доступ без аутентификации</li>
     *   <li>POST /user/register — без аутентификации (регистрация)</li>
     *   <li>POST /user/auth — без аутентификации (авторизация)</li>
     *   <li>Все остальные эндпоинты (/user/**, /booking/**) — требуют аутентификации</li>
 * </ul>
     *
     * @param http           объект HttpSecurity для настройки
     * @param entryPoint     точка входа для обработки ошибок аутентификации
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

                        // Публичный доступ к регистрации и авторизации (без токена)
                        .requestMatchers("/user/register").permitAll()
                        .requestMatchers("/user/auth").permitAll()

                        // Все остальные запросы к API требуют аутентификации
                        .requestMatchers("/user/**").authenticated()
                        .requestMatchers("/booking/**").authenticated()

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
