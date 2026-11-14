package com.skillfactory.luzhin.hotelservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Базовая конфигурация безопасности для Hotel Service.
 * Временно отключает аутентификацию и метод безопасности для тестирования (JWT-безопасность реализуется в Этапе 6).
 *
 * @see com.skillfactory.luzhin.hotelservice.config.JwtSecurityConfig
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Настройка базовой цепочки фильтров безопасности.
     * Временно отключает CSRF и аутентификацию для упрощения тестирования.
     * В продакшене используется JWT-валидация (JwtSecurityConfig).
     * Метод безопасность (@PreAuthorize) также отключена — будет реализована в Этапе 6.
     *
     * @param http объект HttpSecurity для настройки
     * @return настроенная SecurityFilterChain
     * @throws Exception ошибка при настройке безопасности
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
