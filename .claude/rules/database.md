---
paths: "src/main/resources/db/migration/**/*.sql, src/main/resources/application.yml, src/main/resources/application*.yml, docker-compose*.yaml, compose.yaml"
---

# Database + Flyway Conventions

## Источник истины — Flyway

`src/main/resources/db/migration/V{N}__description.sql` — единственное место, где меняется схема.

Hibernate в режиме `validate` — если entity не совпадает с миграцией, приложение **не стартует**. Это by design.

## Naming

- `V1__init.sql` — initial schema
- `V2__seed.sql` — seed data
- `V3__fix_*.sql` — bugfix
- `V4__add_*.sql` — new feature

**Не редактируй уже применённые миграции** (V1, V2) — пиши новую `V{N+1}__*.sql`. Даже для bugfix'а seed-данных.

## CHECK constraints

Все деньги и количества — с CHECK:
- `price >= 0`
- `stock >= 0`
- `quantity > 0`

## CASCADE

- `cart_items.user_id` → `users` (CASCADE — удалили юзера, удалилась корзина)
- `cart_items.book_id` → `books` (CASCADE)
- `orders.user_id` → `users` (CASCADE)
- `orders.id` → `order_items` (CASCADE)
- `order_items.book_id` → `books` (**НЕ** CASCADE — нельзя удалить книгу из заказа)

## Seed users

`V2__seed.sql` создаёт `admin` (ADMIN) и `ivan` (USER) с паролем `password`. Если меняешь seed — не забудь про V3 fix (правильный bcrypt-хеш для "password").

## Datasource (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydatabase
    username: myuser
    password: secret
```

## Docker

```bash
docker compose up -d postgres   # поднять БД
docker compose down             # остановить
docker compose down -v          # полный сброс (drop volume)
```

`spring-boot-docker-compose` НЕ используется — конфликт с нативным PostgreSQL 16 на :5432.

## Testcontainers в тестах

```java
@Container @ServiceConnection
static PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));
```

Использует `@ServiceConnection` из `spring-boot-testcontainers` (Spring Boot 3.1+). Не нужно вручную прописывать `spring.datasource.*` для тестов.
