-- Admin user для тестирования
-- Роль ADMIN позволяет создавать отели, управлять номерами и пользователями
-- H2 совместимый синтаксис (не поддерживает ON CONFLICT)
INSERT INTO users (username, password, role, created_at)
SELECT 'admin', '$2a$10$M9GyEEJqJHH8AA.E3gXgzuVK5T4PrXDmpQcPSGTIPATHSSBOQ1pq.', 'ADMIN', CURRENT_TIMESTAMP()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
