-- Фикс seed паролей: предыдущий хэш в V2 был некорректный (не соответствовал "password").
-- Обновляем admin и ivan на правильный BCrypt-хэш для пароля "password".

UPDATE users
   SET password = '$2a$10$DY5JlAcppznurdIXxiYc1eu5.JKk.MrczqQIJJ03GhC3TyczkSN5a'
 WHERE username IN ('admin', 'ivan');
