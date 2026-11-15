package com.skillfactory.luzhin.hotelservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Фильтр аутентификации на основе JWT-токенов.
 * Извлекает Bearer-токен из заголовка Authorization, валидирует его через JwtUtil
 * и устанавливает контекст аутентификации в SecurityContext.
 *
 * <p>Фильтр работает как OncePerRequestFilter — гарантируется однократное выполнение
 * на каждый HTTP-запрос. Если токен не найден или невалиден — аутентификация
 * не устанавливается, а запрос продолжает обрабатываться (для публичных эндпоинтов).
 *
 * @see JwtUtil
 * @see org.springframework.security.core.context.SecurityContextHolder
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Утилита для работы с JWT-токенами.
     */
    private final JwtUtil jwtUtil;

    /**
     * Префикс Bearer-токена в заголовке Authorization.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Имя заголовка Authorization.
     */
    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;

    /**
     * Выполняет фильтрацию запроса: извлекает JWT-токен из заголовка Authorization,
     * валидирует его и устанавливает аутентификацию в SecurityContext.
     *
     * @param request     HTTP-запрос
     * @param response    HTTP-ответ
     * @param filterChain цепочка фильтров для продолжения обработки запроса
     * @throws ServletException ошибка сервлета
     * @throws IOException      ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        // Проверяем наличие и формат заголовка Authorization
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No Bearer token found in Authorization header for request: {} {}",
                    request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Извлекаем токен (убираем префикс "Bearer ")
        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            // Валидируем токен через JwtUtil
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token for request: {} {}",
                        request.getMethod(), request.getRequestURI());
                sendUnauthorizedResponse(response, "Invalid or expired token");
                return;
            }

            // Извлекаем имя пользователя и роль из токена
            String username = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);

            log.debug("JWT token validated successfully for user={}, role={}", username, role);

            // Создаём объект аутентификации с ролью и устанавливаем в SecurityContext
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("Error processing JWT token for request: {} {}: {}",
                    request.getMethod(), request.getRequestURI(), e.getMessage());
            sendUnauthorizedResponse(response, "Token processing error: " + e.getMessage());
            return;
        }

        // Продолжаем обработку запроса следующей цепочкой фильтров
        filterChain.doFilter(request, response);
    }

    /**
     * Отправляет HTTP-ответ с кодом 401 Unauthorized.
     *
     * @param response  HTTP-ответ
     * @param errorMessage описание ошибки для клиента
     * @throws IOException ошибка ввода-вывода при записи ответа
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + errorMessage + "\"}");
    }

    /**
     * Пропускает запросы к Swagger/OpenAPI-ресурсам без проверки JWT-токена.
     * Это необходимо, чтобы документация API была доступна без аутентификации.
     *
     * @param request HTTP-запрос
     * @return true, если запрос должен быть пропущен без аутентификации; false в противном случае
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/swagger-ui.html")
                || path.equals("/actuator/health")
                || path.equals("/h2-console");
    }
}
