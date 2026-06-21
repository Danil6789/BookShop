# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Что это за проект

**BookShop** — учебный проект (производственная практика). Клиент-серверное приложение: пользователи регистрируются, смотрят каталог, кладут в корзину, оформляют заказы; админ управляет книгами/категориями. Полный план — в `plan.md`.

## Critical Constraints

- **Spring Boot 4.1.0** — Jackson пакет переименован: `tools.jackson.*` (НЕ `com.fasterxml.jackson.*`). Тестовые аннотации — `org.springframework.boot.webmvc.test.autoconfigure.*`.
- **JPA в режиме `validate`** — entity ОБЯЗАНЫ совпадать со схемой Flyway. При расхождении приложение не стартует. Меняй сущность + миграцию вместе.
- **Hibernate `open-in-view: false`** — lazy-отношения НЕ доступны в контроллерах. Всегда подгружай в service через `JOIN FETCH` или DTO-проекцию.
- **JWT role invariant:** `JwtService` хранит role **без** `ROLE_` префикса, фильтр добавляет `ROLE_` сам. `.hasRole("ADMIN")` работает корректно. Не ломай этот контракт.
- **Не редактируй применённые миграции** (V1, V2) — пиши новую `V{N+1}__*.sql`.
- **API доки:** Swagger UI на `http://localhost:8080/swagger-ui/index.html` (НЕ `/swagger-ui.html`).
- **День 1-4 Part 1 ✅, День 4 Part 2 📌** (admin write для books/categories) — см. `plan.md` status block.

## Стек

- **Backend:** Java 21 + Spring Boot 4.1.0 + Spring Data JPA + Spring Security + Spring WebMVC
- **БД:** PostgreSQL 16 (через `compose.yaml`)
- **Миграции:** Flyway (`flyway-core` + `flyway-database-postgresql`)
- **Auth:** JWT (jjwt 0.12.6) + BCrypt (strength 10)
- **Mappers:** MapStruct 1.6.3 (с `lombok-mapstruct-binding`)
- **API docs:** springdoc-openapi 3.0.3
- **Тесты:** JUnit 5 + Mockito + Testcontainers (backend)

## Команды

```bash
./gradlew build              # компиляция + тесты
./gradlew bootRun            # запуск backend на :8080
./gradlew test               # только тесты
./gradlew test --tests "org.example.bookshop.SomeTest"      # один тест-класс
./gradlew compileJava        # быстрая проверка компиляции

docker compose up -d postgres        # поднять БД (5432)
docker compose down                  # остановить
docker compose down -v               # полный сброс (drop volume)
```

## Архитектура (высокоуровнево)

Пакеты в `org.example.bookshop`:

```
user/         User entity + repo
catalog/      Category, Book entities + repos
cart/         CartItem entity + repo
order/        Order, OrderItem entities + OrderStatus enum + repos
config/       security/, swagger/
controller/   Api интерфейсы + impl/ реализации
service/      бизнес-логика (@Transactional)
dto/          request/response records
mapper/       MapStruct интерфейсы
exception/    GlobalExceptionHandler
BookShopApplication.java
```

## Субагенты

Специализированные субагенты в `C:\Users\sotni\.claude\agents\`. Каждый знает конвенции своего слоя.

| Субагент | Когда звать |
|---|---|
| `entity-agent` | Создание/изменение `@Entity` |
| `repository-agent` | `JpaRepository` + `@Query` |
| `service-agent` | Бизнес-логика, `@Transactional` |
| `controller-agent` | REST эндпоинты (Api+Impl) |
| `dto-mapper-agent` | DTO records + MapStruct мапперы |
| `security-agent` | Spring Security, JWT, BCrypt, CORS |
| `migration-agent` | Flyway V*__*.sql |
| `test-agent` | JUnit 5 + Mockito + Testcontainers |

**Workflow:**
- Меняю БД → `migration-agent` → `entity-agent` → `repository-agent`
- Новый эндпоинт → `dto-mapper-agent` → `service-agent` → `controller-agent`
- Тесты → `test-agent`

## Когда использовать Plan Agent

✅ Новые модули, изменения архитектуры, интеграции, миграции данных, multi-step (>3 файлов).
❌ Typos, single-line, simple renames, добавление логов.

## Стиль кода

- DTO — Java records, в подпакетах `dto/{domain}/`.
- Мапперы — **MapStruct** интерфейсы в `mapper/`.
- REST — `/api/...`, Api+Impl split (Swagger аннотации на `*Api` интерфейсах, не на impl).
- Ошибки — единый `ApiError` JSON через `GlobalExceptionHandler`.
- Lombok на `@Entity`: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.

## Modular Docs (правила по компонентам)

| Файл | Когда загружается |
|---|---|
| `.claude/rules/database.md` | SQL миграции, `application.yml`, compose |
| `.claude/rules/jpa.md` | `@Entity`, `@ManyToOne`, relations, equals/hashCode |
| `.claude/rules/security.md` | SecurityConfig, JWT, AuthController, Swagger |

## Что осталось

- ✅ Spring Security (JWT, BCrypt, CORS) — Day 2
- ✅ `GlobalExceptionHandler` для auth ошибок
- ✅ REST-контроллеры каталога (GET /api/categories, GET /api/books) — Day 3
- ✅ Cart endpoints + service — Day 4 Part 1
- 📌 Admin write для books/categories (POST/PUT/DELETE) — Day 4 Part 2
- 📌 Order checkout со списанием stock — Day 5-6
- 📌 Frontend Angular 17 — Day 8-10

## Файлы, которые не трогаем

`HELP.md`, `.gradle/`, `build/`, `compose.yaml` (только при добавлении новых сервисов).
