-- Тестовые данные
-- Пароль для всех юзеров: "password"
-- BCrypt-хеш: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

INSERT INTO users (username, password, role) VALUES
                                                 ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
                                                 ('ivan',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER');

INSERT INTO categories (name) VALUES
                                  ('Художественная литература'),
                                  ('Фантастика'),
                                  ('Детектив'),
                                  ('Учебники'),
                                  ('Детские книги');

INSERT INTO books (title, description, price, stock, category_id, cover_url) VALUES
                                                                                 ('Мастер и Маргарита',               'Роман Михаила Булгакова.',              650.00, 10, 1, 'book-1.jpg'),
                                                                                 ('1984',                             'Роман Джорджа Оруэлла.',                550.00, 15, 2, 'book-2.jpg'),
                                                                                 ('Шерлок Холмс',                    'Собрание рассказов Артура Конан Дойла.', 720.00,  8, 3, 'book-3.jpg'),
                                                                                 ('Высшая математика',               'Учебник для вузов.',                    1200.00, 5, 4, 'book-4.jpg'),
                                                                                 ('Гарри Поттер и философский камень','Первая книга серии Дж. К. Роулинг.',    850.00, 20, 5, 'book-5.jpg'),
                                                                                 ('Война и мир',                     'Роман Льва Толстого.',                  1500.00, 3, 1, 'book-6.jpg'),
                                                                                 ('Дюна',                             'Роман Фрэнка Герберта.',                950.00, 12, 2, 'book-7.jpg'),
                                                                                 ('Десять негритят',                 'Роман Агаты Кристи.',                   480.00,  7, 3, 'book-8.jpg');