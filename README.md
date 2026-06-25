# BookShop

Книжный магазин — учебный клиент-серверный проект (производственная практика).
Angular SPA + Spring Boot REST API + PostgreSQL, JWT-аутентификация, роли USER / ADMIN.

## Возможности

- Регистрация и вход по JWT
- Каталог книг с поиском по названию/автору и фильтром по категории
- Страница книги с обложкой, описанием, остатком на складе
- Корзина: добавить, изменить количество, удалить позицию, очистить
- Оформление заказа из корзины с атомарным списанием со склада
- Список заказов пользователя и история заказов (админ видит все)
- Смена статуса заказа (NEW → PAID → SHIPPED → DELIVERED / CANCELLED) — админ
- Админ-панель: CRUD для книг и категорий, загрузка обложек (JPEG/PNG/WebP до 5 МБ)
- OpenAPI/Swagger UI с русскоязычными описаниями всех эндпоинтов

## Стек

| Слой | Технология |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0 (WebMVC, Data JPA, Security, Validation) |
| БД | PostgreSQL 16 (через `docker-compose.yaml`) |
| Миграции | Flyway (`flyway-core` + `flyway-database-postgresql`) |
| ORM | Hibernate, режим `validate` (entity = схема) |
| Аутентификация | JWT (`jjwt 0.12.6`) + BCrypt (strength 10) |
| Маппинг | MapStruct 1.6.3 |
| API-документация | springdoc-openapi 3.0.3 |
| Frontend | Angular 17 (standalone, signals), Bootstrap 5 (отдельный репозиторий) |
| Сборка backend | Gradle (`./gradlew`) |
| Тесты | JUnit 5, Mockito, Testcontainers (`@ServiceConnection`) |

## Структура репозитория

```
BookShop/
├── src/main/java/org/example/bookshop/
│   ├── BookShopApplication.java
│   ├── config/         Security, JWT, CORS, OpenAPI
│   ├── constant/       ApiPath — константы эндпоинтов
│   ├── controller/     *Api интерфейсы + impl/ реализации (Swagger на интерфейсах)
│   ├── dto/            request/response records по доменам
│   ├── entity/         JPA сущности (User, Book, Category, CartItem, Order, OrderItem)
│   ├── exception/      Доменные исключения
│   ├── handler/        GlobalExceptionHandler — единый формат ApiError
│   ├── mapper/         MapStruct интерфейсы
│   ├── repository/     Spring Data JPA репозитории
│   └── service/        Бизнес-логика (@Transactional)
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/   V1__init.sql, V2__seed.sql, V3__fix_seed_passwords.sql, V4__fix_book_cover_urls.sql
│   └── static/         Обложки-сиды
├── src/test/java/      Интеграционные тесты на Testcontainers
├── docker-compose.yaml PostgreSQL 16
└── build.gradle
```

Frontend в отдельном репозитории: `C:\Angular\BookShop-Front\` (Angular 17, standalone, signals, functional interceptors/guards, Bootstrap 5).

## Быстрый старт

### 1. Поднять базу данных

```bash
docker compose up -d postgres        # стартует на :5432 (user=myuser, db=mydatabase, password=secret)
```

Дождаться готовности (healthcheck), обычно 2–5 секунд.

### 2. Запустить backend

```bash
./gradlew bootRun                     # стартует на http://localhost:8080
```

При старте Flyway автоматически применит все миграции из `src/main/resources/db/migration/` и засеет тестовые данные.

Swagger UI: <http://localhost:8080/swagger-ui/index.html>

### 3. Запустить frontend (отдельно)

```bash
cd C:\Angular\BookShop-Front
npm install
npx ng serve --port 4200
```

Фронт стартует на <http://localhost:4200>. CORS для этого origin уже разрешён в `SecurityConfig`.

## Тестовые пользователи

После применения миграции `V2__seed.sql` доступны:

| Логин  | Пароль    | Роль  |
|--------|-----------|-------|
| `admin` | `password` | ADMIN |
| `ivan`  | `password` | USER  |

## REST API

Все эндпоинты под `/api`. Защищённые требуют заголовок `Authorization: Bearer <jwt>`.

### Аутентификация

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| POST | `/api/auth/register` | публичный | Регистрация. Возвращает данные пользователя |
| POST | `/api/auth/login` | публичный | Вход. Возвращает JWT и срок действия в секундах |

### Категории

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/categories` | публичный | Список категорий |
| GET | `/api/categories/{id}` | публичный | Категория по id |
| POST | `/api/categories` | ADMIN | Создать |
| PUT | `/api/categories/{id}` | ADMIN | Обновить |
| DELETE | `/api/categories/{id}` | ADMIN | Удалить |

### Книги

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/books` | публичный | Список книг с пагинацией |
| GET | `/api/books/search` | публичный | Поиск: `?query=...&categoryId=...` |
| GET | `/api/books/{id}` | публичный | Карточка книги |
| POST | `/api/books` | ADMIN | Создать книгу |
| PUT | `/api/books/{id}` | ADMIN | Обновить |
| DELETE | `/api/books/{id}` | ADMIN | Удалить |

### Корзина (требуется JWT)

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/cart` | Содержимое корзины текущего пользователя |
| POST | `/api/cart/items` | Добавить книгу (`{ bookId, quantity }`) |
| PUT | `/api/cart/items/{bookId}` | Изменить количество |
| DELETE | `/api/cart/items/{bookId}` | Удалить позицию |
| DELETE | `/api/cart` | Очистить корзину |

### Заказы (требуется JWT)

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| POST | `/api/orders` | USER | Оформить заказ из корзины (атомарно списывает stock) |
| GET | `/api/orders` | USER/ADMIN | USER видит свои, ADMIN — все |
| GET | `/api/orders/{id}` | USER/ADMIN | Детали заказа (USER — только свои) |
| PATCH | `/api/orders/{id}/status` | ADMIN | Сменить статус |
| DELETE | `/api/orders/{id}` | ADMIN | Удалить заказ |

### Загрузка файлов (ADMIN)

| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/uploads` | multipart/form-data, поле `file`, тип JPEG/PNG/WebP, до 5 МБ. Возвращает публичный URL для `book.coverUrl` |

## База данных

Шесть таблиц, источник истины — миграции Flyway. Hibernate в режиме `validate` гарантирует, что сущности совпадают со схемой: при расхождении приложение не стартует.

| Таблица | Назначение |
|---|---|
| `users` | пользователи (USER / ADMIN), BCrypt-хеш пароля |
| `categories` | жанры книг |
| `books` | книги с обложкой, ценой, остатком на складе |
| `cart_items` | корзина пользователя |
| `orders` | заказы (статус, адрес, итоговая сумма) |
| `order_items` | позиции заказа |

CHECK-ограничения на деньги и количества (`price >= 0`, `stock >= 0`, `quantity > 0`).
Каскадное удаление: `cart_items.user_id → users` (CASCADE), `order_items.book_id → books` (**без** CASCADE — нельзя удалить книгу из истории заказа).

## Разработка

### Тесты

```bash
./gradlew test                                                    # все тесты
./gradlew test --tests "org.example.bookshop.SomeTest"            # один класс
./gradlew test --tests "org.example.bookshop.SomeTest.method"     # один метод
```

Используется Testcontainers (`@ServiceConnection`) — каждый тест поднимает чистый PostgreSQL в Docker.

### Сборка

```bash
./gradlew build           # компиляция + тесты + JAR
./gradlew compileJava     # только проверка компиляции
```

### Полезные команды

```bash
docker compose down       # остановить БД
docker compose down -v    # остановить и удалить том (полный сброс)
```

## Соглашения

- DTO — Java `record` в подпакетах `dto/{domain}/`
- Мапперы — MapStruct интерфейсы в `mapper/`, `componentModel = SPRING`, `unmappedTargetPolicy = IGNORE`
- REST — `/api/...`, разделение `*Api` интерфейс + `impl/` реализация; Swagger-аннотации на интерфейсе, не на impl
- Ошибки — единый JSON `ApiError` через `GlobalExceptionHandler`
- JPA-сущности — Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`), `open-in-view: false` — все lazy-связи подгружаются в сервисах через `JOIN FETCH` или DTO-проекцию
- JWT role invariant — `JwtService` хранит роль **без** префикса `ROLE_`, фильтр добавляет префикс сам

## Связанные Claude Code Skills

В проекте настроены специализированные субагенты (`~/.claude/agents/`), которые знают конвенции своего слоя:

| Субагент | Когда звать |
|---|---|
| `entity-agent` | Создание/изменение `@Entity` |
| `repository-agent` | `JpaRepository` + `@Query` |
| `service-agent` | Бизнес-логика, `@Transactional` |
| `controller-agent` | REST эндпоинты (Api+Impl) |
| `dto-mapper-agent` | DTO records + MapStruct |
| `security-agent` | Spring Security, JWT, BCrypt, CORS |
| `migration-agent` | Flyway `V*__*.sql` |
| `exception-agent` | Кастомные исключения и `GlobalExceptionHandler` |
| `test-agent` | JUnit 5 + Mockito + Testcontainers |
| `architecture-agent` | Структура пакетов, размещение файлов |

**Типичный workflow:**

- Меняю БД → `migration-agent` → `entity-agent` → `repository-agent`
- Новый эндпоинт → `dto-mapper-agent` → `service-agent` → `controller-agent`
- Тесты → `test-agent`

## Статус проекта

Учебная практика завершена (День 1–8): backend, frontend, основные сценарии end-to-end работают. Подробный план — в `plan.md`, инструкции для ИИ-агента — в `CLAUDE.md`.

## Лицензия

Учебный проект. Используй как хочешь.