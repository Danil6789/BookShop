-- 1. Пользователи
CREATE TABLE users (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'USER'
);

-- 2. Категории
CREATE TABLE categories (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 3. Книги
CREATE TABLE books (
    id          BIGSERIAL      PRIMARY KEY,
    title       VARCHAR(255)   NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock       INT            NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category_id BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    cover_url   VARCHAR(500)
);

CREATE INDEX idx_books_category ON books(category_id);
CREATE INDEX idx_books_title    ON books(title);

-- 4. Корзина
CREATE TABLE cart_items (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id  BIGINT    NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    quantity INT       NOT NULL DEFAULT 1 CHECK (quantity > 0),
    UNIQUE (user_id, book_id)
);

CREATE INDEX idx_cart_user ON cart_items(user_id);

-- 5. Заказы
CREATE TABLE orders (
    id           BIGSERIAL      PRIMARY KEY,
    user_id      BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_amount NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_orders_user ON orders(user_id);

-- 6. Позиции заказа
CREATE TABLE order_items (
    id                BIGSERIAL      PRIMARY KEY,
    order_id          BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id           BIGINT         NOT NULL REFERENCES books(id),
    quantity          INT            NOT NULL CHECK (quantity > 0),
    price_at_purchase NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
