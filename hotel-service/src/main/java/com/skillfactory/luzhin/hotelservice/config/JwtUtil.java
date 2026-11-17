package com.skillfactory.luzhin.hotelservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Утилита для работы с JWT-токенами.
 * Обеспечивает парсинг, валидацию и извлечение данных из JWT-токенов.
 * Использует алгоритм HS256 (HMAC with SHA-256) для подписи и верификации.
 *
 * <p>JWT-секрет разделяется между всеми сервисами системы (Hotel Service и Booking Service)
 * для обеспечения единой схемы аутентификации в микросервисной архитектуре.
 *
 * @see io.jsonwebtoken.Jwts
 */
@Component
public class JwtUtil {

    /**
     * Секретный ключ для подписи и верификации JWT-токенов.
     * Загружается из переменной окружения JWT_SECRET, либо используется значение по умолчанию.
     */
    private final SecretKey signingKey;

    /**
     * Максимальное время жизни токена в миллисекундах (по умолчанию — 1 час).
     */
    private final long expirationMs;

    /**
     * Конструктор с автоматической загрузкой конфигурации JWT из application.yml.
     *
     * @param secretBase64 Base64-закодированный секретный ключ для подписи токенов
     * @param expirationMs максимальное время жизни токена в миллисекундах
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expirationMs = expirationMs;
    }

    /**
     * Извлекает имя пользователя (subject) из JWT-токена.
     *
     * @param token JWT-токен
     * @return имя пользователя, извлечённое из claims токена
     * @throws JwtException если токен невалиден или имя пользователя не найдено
     */
    public String getUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Извлекает роль пользователя из JWT-токена.
     * Роль хранится в claims под ключом "role" и имеет формат "ROLE_USER" или "ROLE_ADMIN".
     *
     * @param token JWT-токен
     * @return роль пользователя (например, "ROLE_USER" или "ROLE_ADMIN")
     * @throws JwtException если токен невалиден или роль не найдена
     */
    public String getRole(String token) {
        Claims claims = extractAllClaims(token);
        Object role = claims.get("role");
        if (role == null) {
            throw new JwtException("Role not found in token");
        }
        return role.toString();
    }

    /**
     * Извлекает дату истечения токена из JWT-токена.
     *
     * @param token JWT-токен
     * @return дата истечения токена
     * @throws JwtException если токен невалиден или expiration не найдено
     */
    public Date getExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Проверяет, истёк ли срок действия JWT-токена.
     *
     * @param token JWT-токен
     * @return true, если токен истёк; false в противном случае
     */
    public boolean isTokenExpired(String token) {
        return getExpirationDate(token).before(new Date());
    }

    /**
     * Валидирует JWT-токен: проверяет подпись, срок действия и наличие имени пользователя.
     *
     * @param token JWT-токен для валидации
     * @return true, если токен валиден; false в противном случае
     */
    public boolean isTokenValid(String token) {
        try {
            // Проверяем, что токен не истёк
            if (isTokenExpired(token)) {
                return false;
            }
            // Проверяем, что можно извлечь имя пользователя (токен валиден и подписан правильным ключом)
            getUsername(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Извлекает конкретный claim из JWT-токена с использованием передовой функции.
     *
     * @param token JWT-токен
     * @param claimsResolver функция для извлечения требуемого claim
     * @param <T> тип возвращаемого claim
     * @return извлечённый claim
     * @throws JwtException если токен невалиден
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлекает все claims из JWT-токена.
     *
     * @param token JWT-токен
     * @return объект Claims, содержащий все данные токена
     * @throws JwtException если токен невалиден или подпись не совпадает
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
