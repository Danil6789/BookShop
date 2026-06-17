# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Что это за проект

**BookShop** — учебный проект (производственная практика, тема «Книжный магазин»). Клиент-серверное приложение: пользователи регистрируются, просматривают каталог книг, кладут в корзину, оформляют заказы; админ управляет книгами и категориями.

Полный план (архитектура, API, разбивка по дням, маппинг на компетенции ПК) — в `plan.md`.

## Стек

- **Backend:** Java 21 + Spring Boot 4.1.0 + Spring Data JPA + Spring Security + Spring WebMVC
- **БД:** PostgreSQL 16 (через `compose.yaml`)
- **Миграции:** Flyway (`flyway-core` + `flyway-database-postgresql`)
- **Auth:** JWT (jjwt), BCrypt (планируется)
- **Frontend:** Angular 17 standalone + Bootstrap 5 (планируется, папка `bookshop-frontend/`)
- **Тесты:** JUnit 5 + Testcontainers (backend), Jasmine/Karma (frontend)

## Команды

Все команды выполняются из корня проекта.

```bash
./gradlew build              # компиляция + тесты
./gradlew bootRun            # запуск backend на http://localhost:8080
./gradlew test               # только тесты
./gradlew test --tests "org.example.bookshop.SomeTest"      # один тест-класс
./gradlew test --tests "org.example.bookshop.SomeTest.method" # один метод
./gradlew compileJava        # только компиляция (быстрая проверка)
./gradlew bootJar            # собрать JAR в build/libs/
```

### База данных

```bash
docker compose up -d postgres        # поднять PostgreSQL (5432, БД mydatabase)
docker compose down                  # остановить
docker compose down -v               # остановить и удалить том (полный сброс)
```

`spring-boot-docker-compose` сам стартует postgres при `bootRun`. Ручной запуск не обязателен, но полезен для дебага.

## Модель данных (актуальная)

6 таблиц, схема в `src/main/resources/db/migration/V1__init.sql`. Источник истины — миграция Flyway, JPA в режиме `validate` (entity ↔ schema).

| Таблица | Поля |
|---|---|
| `users` | id, username (UNIQUE), password, role (`USER`/`ADMIN`, default `USER`) |
| `categories` | id, name (UNIQUE) |
| `books` | id, title, description, price (NUMERIC 10,2), stock, category_id → categories, cover_url |
| `cart_items` | id, user_id → users (CASCADE), book_id → books (CASCADE), quantity; UNIQUE(user_id, book_id) |
| `orders` | id, user_id → users (CASCADE), total_amount, status (default `PENDING`) |
| `order_items` | id, order_id → orders (CASCADE), book_id → books, quantity, price_at_purchase |

CHECK constraints: `price >= 0`, `stock >= 0`, `quantity > 0`.

## Архитектура

Пакеты в `org.example.bookshop`:

```
user/         User entity + UserRepository
catalog/      Category, Book entities + CategoryRepository, BookRepository
cart/         CartItem entity + CartItemRepository
order/        Order, OrderItem entities + OrderStatus enum + OrderRepository, OrderItemRepository
BookShopApplication.java   точка входа
```

### Реализованные best-practice паттерны в сущностях

- **Lombok:** `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` на классе.
- **Equals/HashCode:** `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include` только на поле `id`. Отношения автоматически исключаются.
- **toString:** Lombok `@ToString` + `@ToString.Exclude` на каждом `@ManyToOne` / `@OneToMany` — чтобы lazy-загрузка не дёргалась.
- **ID:** `@GeneratedValue(strategy = GenerationType.IDENTITY)` для BIGSERIAL.
- **BigDecimal:** всегда с `precision = 10, scale = 2`.
- **Relations:** `@ManyToOne(fetch = FetchType.LAZY)` + явный `@JoinColumn(name = "...")`.
- **Enums:** `@Enumerated(EnumType.STRING)` для `User.role` и `Order.status`.
- **Unique constraint:** `@UniqueConstraint` в `@Table` для `cart_items (user_id, book_id)`.
- **Defaults на уровне Java:** `@PrePersist` для `role` и `status` (на случай если поле не задано вручную).

### Конфигурация (`application.yml`)

- `spring.datasource` — `jdbc:postgresql://localhost:5432/mydatabase`, myuser/secret.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate только проверяет, что entity совпадает со схемой Flyway.
- `spring.jpa.open-in-view=false` — отключает anti-pattern открытой сессии в контроллерах.
- `spring.flyway.locations=classpath:db/migration` — путь к миграциям.

## Что осталось (по плану)

- Spring Security (JWT, BCrypt, CORS для Angular на 4200)
- REST-контроллеры + DTO + ручные мапперы
- Сервисный слой с `@Transactional` (особенно для Order — списание stock)
- Глобальный обработчик ошибок (`GlobalExceptionHandler`)
- Frontend на Angular

## Стиль кода

- Lombok аннотации на класс — стандартный набор как в `Book.java`.
- Мапперы Entity ↔ DTO — **ручные**, не MapStruct.
- DTO — отдельные record-классы или обычные классы в подпакете `dto/`.
- Названия пакетов lowercase, классы PascalCase, методы camelCase.
- Все новые REST-эндпоинты — под `/api/...`. Ошибки — единый формат `ApiError` (см. план).

## Файлы, которые не трогаем

- `HELP.md` — стандартный Spring Boot reference.
- `compose.yaml` — корректируется только при добавлении новых сервисов (backend добавится в День 9).
- `.gradle/`, `build/` — генерируются автоматически.
