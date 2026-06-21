# Книжный магазин (BookShop) — План реализации

> **Контекст:** Учебная производственная практика, тема «Книжный магазин». Дедлайн — 10 дней. Цель — рабочее клиент-серверное приложение, покрывающее компетенции ПК-2.2 … ПК-7.6. Минимально работающая версия.
>
> **Стек:** Backend — Java 21 + Spring Boot 4.1.0 + Spring Data JPA + Spring Security (JWT) + Flyway. Frontend — Angular 17 (standalone components). БД — PostgreSQL 16. Сборка backend+БД — Docker Compose. Тесты — JUnit 5 + Testcontainers (backend), Jasmine/Karma (frontend).

> **Статус по дням:** День 1 — ✅ завершён. День 2 — ✅ завершён (см. блок в конце Дня 2). День 3+ — в работе.

---

## 1. Архитектура (высокоуровневая)

```
┌──────────────────┐   HTTP/JSON (JWT)    ┌──────────────────────┐    JDBC    ┌──────────────────┐
│  Angular SPA     │ ───────────────────► │  Spring Boot REST    │ ─────────► │  PostgreSQL 16   │
│  (port 4200)     │ ◄─────────────────── │  (port 8080)         │ ◄───────── │  (port 5432)     │
└──────────────────┘                       └──────────────────────┘            └──────────────────┘
        │                                           │
        │ статика через nginx                        │ логи, метрики
        ▼                                           ▼
   dev-сервер `ng serve`                       Actuator + Logback
```

**Модули backend (один Maven/Gradle модуль, пакеты):**
- `config` — Security, JWT, CORS, OpenAPI
- `user` — User entity, repository, service, controller (auth + profile)
- `catalog` — Category, Book entities, repository, service, controller
- `cart` — CartItem entity, repository, service, controller
- `order` — Order, OrderItem entities, repository, service, controller
- `common` — ошибки, DTO, мапперы, утилиты

**Spring Modulith** уже подключён в `build.gradle`. Используем его **минимально** (просто пакеты как aggregate). Никаких module-info, ApplicationModule-зависимостей — это лишняя сложность на 10 дней. Зависимости из `build.gradle` оставляем как есть, чтобы не ломать старт.

---

## 2. Модель БД (финальная, утверждённая)

> Минимальная схема под сквозной сценарий «зарегистрировался → выбрал книгу → положил в корзину → оформил заказ». Источник истины — `src/main/resources/db/migration/V1__init.sql`. Никаких избыточных полей.

### 2.1 Таблицы

**`users`**
| Колонка | Тип | Ограничения | Зачем |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | суррогатный ключ |
| `username` | `VARCHAR(50)` | UNIQUE, NOT NULL | логин |
| `password` | `VARCHAR(255)` | NOT NULL | BCrypt-хеш (60 + запас) |
| `role` | `VARCHAR(20)` | NOT NULL, DEFAULT `'USER'` | RBAC через enum (`USER`/`ADMIN`) |

**`categories`**
| Колонка | Тип | Ограничения | Зачем |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `name` | `VARCHAR(100)` | UNIQUE, NOT NULL | название категории |

**`books`**
| Колонка | Тип | Ограничения | Зачем |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `title` | `VARCHAR(255)` | NOT NULL | |
| `description` | `TEXT` | NULL | |
| `price` | `NUMERIC(10,2)` | NOT NULL, CHECK (price >= 0) | деньги — только DECIMAL |
| `stock` | `INTEGER` | NOT NULL, DEFAULT 0, CHECK (stock >= 0) | остаток на складе |
| `category_id` | `BIGINT` | FK → categories(id) ON DELETE SET NULL | категория (необязательна) |
| `cover_url` | `VARCHAR(500)` | NULL | путь к обложке (`/uploads/covers/{uuid}.jpg`) |

Индексы: `idx_books_category`, `idx_books_title`.

**`cart_items`**
| Колонка | Тип | Ограничения | Зачем |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | NOT NULL, FK → users(id) ON DELETE CASCADE | |
| `book_id` | `BIGINT` | NOT NULL, FK → books(id) ON DELETE CASCADE | |
| `quantity` | `INTEGER` | NOT NULL, DEFAULT 1, CHECK (quantity > 0) | |

UNIQUE (`user_id`, `book_id`) — одна книга = одна строка корзины (увеличение quantity). Индекс: `idx_cart_user`.

**`orders`**
| Колонка | Тип | Ограничения | Зачем |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | NOT NULL, FK → users(id) ON DELETE CASCADE | |
| `total_amount` | `NUMERIC(10,2)` | NOT NULL | денормализация суммы |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'PENDING'` | жизненный цикл (`PENDING`/`PAID`/`SHIPPED`/`CANCELLED`/`COMPLETED`) |

Индекс: `idx_orders_user`.

**`order_items`**
| Колонка | Тип | Ограничения | Зачем |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `order_id` | `BIGINT` | NOT NULL, FK → orders(id) ON DELETE CASCADE | |
| `book_id` | `BIGINT` | NOT NULL, FK → books(id) | не CASCADE — книгу нельзя удалить, если она в заказе |
| `quantity` | `INTEGER` | NOT NULL, CHECK (quantity > 0) | |
| `price_at_purchase` | `NUMERIC(10,2)` | NOT NULL | **заморозка цены** — критично для отчётности |

Индекс: `idx_order_items_order`.

### 2.2 ER-диаграмма (текстом)

```
users 1───* cart_items *───1 books
  │                            │
  │                            │
  1                            1
  │                            │
  *                            *
  orders ──1──* order_items *──┘

categories 1───* books
```

### 2.3 Хранение обложек — локальные файлы + путь в БД

**Решение:** файлы на диске, в БД — только путь. Тип поля `cover_url` — `VARCHAR(512)` (в Java — `String`).

**Структура папок:**
```
BookShop/
├── uploads/                          # runtime, .gitignore
│   └── covers/
│       ├── seed-1.jpg                # копии из static при первом старте
│       └── {uuid}.jpg                # загруженные админом
└── src/main/resources/static/seed-covers/
    ├── book-1.jpg                    # в гите, для seed-данных
    ├── book-2.jpg
    └── ...
```

**Админский endpoint** (расширение `/api/books` POST/PUT):
```java
@PostMapping(value = "/api/admin/books", consumes = MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public BookDto create(@RequestPart("book") @Valid CreateBookRequest req,
                     @RequestPart(value = "cover", required = false) MultipartFile cover) {
    String coverUrl = coverService.save(cover);
    return bookService.create(req, coverUrl);
}
```

**CoverService:**
```java
@Service
@RequiredArgsConstructor
public class CoverService {
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !(ext.matches("jpg|jpeg|png|webp"))) {
            throw new IllegalArgumentException("Unsupported image format");
        }
        String filename = UUID.randomUUID() + "." + ext;
        Path target = Paths.get(uploadDir, "covers", filename).toAbsolutePath();
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        return "/uploads/covers/" + filename;
    }
}
```

**Отдача файлов** — через `WebMvcConfigurer`:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

**`application.properties`:**
```
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
app.upload-dir=uploads
```

**`.gitignore` (добавить):**
```
uploads/
```

**Seed-картинки:** в `V2__seed.sql` для 6 книг указать пути `/seed-covers/book-1.jpg ... book-6.jpg`. `WebConfig` дополнительно отдаёт `classpath:/static/seed-covers/` под префиксом `/seed-covers/**`. Или проще: при старте приложения `@PostConstruct` копирует содержимое `static/seed-covers/` в `uploads/covers/`, если их там нет.

### 2.4 Что НЕ добавляем (YAGNI)
- ❌ `reviews` — не входит в скоуп ПЗ.
- ❌ `payments` — статус «оплачен» ставим вручную в демо (без интеграции).
- ❌ `images` отдельной таблицей — обложка как файл + путь.
- ❌ `audit_log` — лишний слой.
- ❌ Мягкое удаление (soft delete) — не требуется.

---

## 3. API endpoints (контракт)

> Все ответы JSON. Ошибки — единый формат `ApiError { timestamp, status, error, message, path, validationErrors? }`.

### 3.1 Аутентификация (`/api/auth`)
| Метод | URL | Доступ | Тело | Ответ |
|---|---|---|---|---|
| POST | `/api/auth/register` | public | `{username, email, password}` | 201 + `{userId, username, email, role}` |
| POST | `/api/auth/login` | public | `{username, password}` | 200 + `{token, expiresIn, user}` |
| GET  | `/api/auth/me` | USER | — | 200 + user |

### 3.2 Категории (`/api/categories`)
| Метод | URL | Доступ | Описание |
|---|---|---|---|
| GET | `/api/categories` | public | список всех |
| GET | `/api/categories/{id}` | public | одна категория |
| POST | `/api/categories` | ADMIN | создать |
| PUT | `/api/categories/{id}` | ADMIN | обновить |
| DELETE | `/api/categories/{id}` | ADMIN | удалить (если нет книг) |

### 3.3 Книги (`/api/books`)
| Метод | URL | Доступ | Описание |
|---|---|---|---|
| GET | `/api/books` | public | пагинация `?page=0&size=20&sort=title,asc` |
| GET | `/api/books/{id}` | public | детально |
| GET | `/api/books/search` | public | `?q=&categoryId=&minPrice=&maxPrice=` |
| POST | `/api/books` | ADMIN | создать |
| PUT | `/api/books/{id}` | ADMIN | обновить (включая stock) |
| DELETE | `/api/books/{id}` | ADMIN | удалить (если не в заказах) |

### 3.4 Корзина (`/api/cart`)
| Метод | URL | Доступ | Описание |
|---|---|---|---|
| GET | `/api/cart` | USER | моя корзина |
| POST | `/api/cart/items` | USER | `{bookId, quantity}` — добавить/обновить |
| DELETE | `/api/cart/items/{bookId}` | USER | удалить позицию |
| DELETE | `/api/cart` | USER | очистить |

### 3.5 Заказы (`/api/orders`)
| Метод | URL | Доступ | Описание |
|---|---|---|---|
| POST | `/api/orders` | USER | `{shippingAddress}` — оформить из корзины (атомарно списывает stock) |
| GET | `/api/orders` | USER | мои заказы |
| GET | `/api/orders/{id}` | USER (свой) / ADMIN | детально |
| PATCH | `/api/orders/{id}/status` | ADMIN | сменить статус |

---

## 4. Frontend — карта страниц (минимально)

| Путь | Компонент | Доступ | Содержимое |
|---|---|---|---|
| `/` | `CatalogPage` | public | сетка книг + фильтры (категория, цена, поиск) |
| `/books/:id` | `BookDetailPage` | public | обложка, описание, «в корзину» |
| `/login` | `LoginPage` | guest | форма входа |
| `/register` | `RegisterPage` | guest | форма регистрации |
| `/cart` | `CartPage` | USER | список позиций, «Оформить» |
| `/orders` | `OrdersPage` | USER | история заказов |
| `/orders/:id` | `OrderDetailPage` | USER/ADMIN | детали + статус |
| `/admin/books` | `AdminBooksPage` | ADMIN | CRUD книг |
| `/admin/categories` | `AdminCategoriesPage` | ADMIN | CRUD категорий |

**Auth:** `AuthService` хранит JWT в `localStorage`, `authInterceptor` подставляет `Authorization: Bearer ...`, `authGuard` блокирует приватные роуты, `roleGuard` — админские.

**UI-kit:** Angular Material (минимум — кнопки, формы, карточки) или чистый Bootstrap. **Рекомендую Bootstrap 5** — быстрее, меньше зависимостей, не утяжеляет bundle.

---

## 5. План по дням (10 дней)

> Каждый день = один законченный инкремент. В конце каждого дня — `git commit` (conventional commits) и обновление `README.md` (если менялся API).

### День 1 — Фундамент, БД, скелет backend
**Цель:** поднимается backend, подключается к PostgreSQL, можно через psql посмотреть таблицы.

Файлы:
- `build.gradle` — добавить `flyway-core`, `flyway-database-postgresql`, `jjwt-api/impl/jackson`, `springdoc-openapi-starter-webmvc-ui` (опционально)
- `compose.yaml` — оставить postgres, добавить сервис `backend` (опционально, можно позже)
- `src/main/resources/application.properties` — datasource, JPA, Flyway, JWT secret
- `src/main/resources/db/migration/V1__init.sql` — DDL всех таблиц
- `src/main/resources/db/migration/V2__seed.sql` — начальные данные (1 админ, 3 категории, 6 книг)
- `src/main/java/.../common/error/GlobalExceptionHandler.java`
- `src/main/java/.../common/error/ApiError.java`

Шаги:
1. Дописать `application.properties` (URL БД, логин/пароль из compose, `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`).
2. Поднять postgres: `docker compose up -d postgres`.
3. Создать `V1__init.sql` (все CREATE TABLE из раздела 2.1) + индексы.
4. Создать `V2__seed.sql` (1 ADMIN user с BCrypt-хешем пароля `admin123`, категории «Художественная литература / Учебники / Детская литература», 6 книг).
5. Запустить backend: `./gradlew bootRun`. Проверить лог Flyway «Successfully applied 2 migrations». Зайти в БД `psql` и убедиться, что таблицы есть.
6. `GlobalExceptionHandler` — `@RestControllerAdvice` + `MethodArgumentNotValidException` + `ResponseStatusException`. Сделать unit-тест.
7. `git commit -m "chore: configure postgres, flyway, base error handling"`.

**Проверка конца дня:** `./gradlew test` зелёный. `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

### ✅ День 1 — статус: ЗАВЕРШЁН (2026-06-20)

**Что сделано сверх плана:**
- Backend стартует, Flyway применяет V1 + V2, Hibernate validate проходит
- 7 entities + 6 repositories в пакетах user/catalog/cart/order
- Лог: `Started BookShopApplication in 19.476 seconds`
- Компактная сводка: `code.md` в корне проекта

**Отклонения от исходного плана Дня 1:**
- ❌ `GlobalExceptionHandler` — НЕ сделан (перенесён в День 6)
- ❌ `application.properties` → `application.yml` (используется YAML)
- ❌ `compose.yaml` → `docker-compose.yaml` (legacy v1 формат)
- ⚠️ `spring-boot-docker-compose` удалён из `build.gradle` (конфликт с нативным PostgreSQL 16 на порту 5432). БД поднимается руками: `docker compose up -d postgres`
- ⚠️ `spring-boot-flyway` добавлен отдельной зависимостью (в Spring Boot 4.x это не автоконфиг, а отдельный модуль)
- ⚠️ Плагины `org.hibernate.orm` и `org.graalvm.buildtools.native` удалены — ломали Flyway в JVM-режиме

---

### День 2 — User domain + JWT auth
**Цель:** можно зарегистрироваться, залогиниться, получить токен, защищённый эндпоинт работает.

Файлы:
- `user/User.java` (entity)
- `user/UserRepository.java`
- `user/Role.java` (enum)
- `user/dto/RegisterRequest.java`, `LoginRequest.java`, `AuthResponse.java`, `UserDto.java`
- `user/UserService.java`
- `user/AuthController.java`
- `user/AppUserDetails.java` + `AppUserDetailsService.java` (для Spring Security)
- `config/SecurityConfig.java`
- `config/JwtService.java`, `JwtAuthenticationFilter.java`
- Тесты: `UserServiceTest`, `AuthControllerIntegrationTest`

Шаги (TDD где разумно, иначе — реализация + тест):
1. User entity (Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`).
2. `UserRepository extends JpaRepository<User, Long>` + метод `Optional<User> findByUsername(String)`.
3. DTO с `@Valid` (`@NotBlank`, `@Email`, `@Size(min=6, max=64)` для пароля).
4. `UserService.register(...)` — проверка уникальности, BCrypt (`PasswordEncoder` из SecurityConfig), сохранение.
5. `JwtService` — генерация/парсинг токенов (jjwt 0.12.x API, claims `sub=username`, `role`, `exp=now+24h`).
6. `JwtAuthenticationFilter` — `OncePerRequestFilter`, извлекает `Authorization: Bearer ...`, кладёт `Authentication` в `SecurityContextHolder`.
7. `SecurityConfig` — `SecurityFilterChain` bean: `/api/auth/**`, `/api/categories/**` (GET), `/api/books/**` (GET), `/actuator/health` — public; остальное — `authenticated()`. `AuthenticationManager`, `PasswordEncoder` (BCrypt strength 10) — бины. CORS — разрешить `http://localhost:4200`.
8. `AuthController` — 3 endpoint, `@Valid` на входе.
9. Интеграционный тест: `MockMvc` + `@SpringBootTest` + `Testcontainers` (PostgreSQL) — `register → 201`, `login → 200 + token`, `me без токена → 401`, `me с токеном → 200`.
10. `git commit -m "feat(user): registration, login, JWT auth"`.

**Проверка:** через `curl` или Postman — register, login, получить токен, вызвать `/api/auth/me` с заголовком.

---

### ✅ День 2 — статус: ЗАВЕРШЁН (2026-06-21)

**Что сделано:**
- **Spring Security:** `SecurityConfig` (BCrypt strength 10, stateless, CORS для `http://localhost:4200`), `JwtAuthenticationFilter` (`ROLE_` prefix в `SimpleGrantedAuthority`), `JwtService` (jjwt 0.12.6, HS256, min 32 bytes secret)
- **Auth endpoints:** `POST /api/auth/register` (201) + `POST /api/auth/login` (200 + JWT) — через Api+Impl pattern
- **Сервисный слой:** `UserService.register(...)` + `AuthService.login(...)` с `@Transactional` boundaries; BCrypt хеширование паролей
- **DTO + MapStruct:** `UserDto`, `LoginRequest`, `RegisterRequest`, `AuthResponse`, `UserDetailsImpl`; `UserMapper` через MapStruct 1.6.3
- **Error handling:** `GlobalExceptionHandler` для 400/401/403/404/409 → единый `ApiError` JSON
- **Flyway V3:** фикс сломанного bcrypt-хеша в V2 (admin/ivan теперь могут логиниться с паролем `password`)
- **Тесты:** 5 unit (`JwtServiceTest`) + 4 unit (`UserServiceTest` с Mockito) + 5 integration (`AuthControllerIntegrationTest` с Testcontainers+MockMvc) = **15 тестов, BUILD SUCCESSFUL**
- **Swagger UI:** springdoc-openapi 3.0.3, доступен на `http://localhost:8080/swagger-ui/index.html` с JWT Bearer auth scheme (кнопка Authorize)
- **Документация:** CLAUDE.md обновлён (security ✅ done, MapStruct в стеке, "Что осталось" обновлён)

**Архитектурные решения (отступления от исходного плана):**
- ❌ Не сделано: `GET /api/auth/me` — перенесён в День 3 (нет смысла делать изолированно, нужен для фронта с контекстом других endpoints)
- ⚠️ Api+Impl pattern (из cloud-storage): `AuthApi` interface + `AuthController` impl — единообразно с другими контроллерами проекта
- ⚠️ `MapStruct` 1.6.3 использован для маппинга (изначально план был «ручные мапперы» — пересмотрели в сторону MapStruct для type safety)
- ⚠️ `springdoc-openapi` подключён уже в День 2 (а не в День 6) — для ручного тестирования через Swagger UI
- ⚠️ `application.yml` хранит JWT-секрет с дефолтом, переопределяется через `JWT_SECRET` env var

**Что НЕ сделано** (перенесено):
- `GlobalExceptionHandler` ещё не покрывает 422 (бизнес-валидация вроде insufficient stock) — добавим когда появятся соответствующие исключения в Day 4-5
- `@PreAuthorize("hasRole('ADMIN')")` не расставлен — нет защищаемых эндпоинтов в коде, добавим в День 4 когда появятся POST/PUT/DELETE на `/api/books` и `/api/categories`

---

### День 3 — Catalog (categories + books) — read
**Цель:** публичные GET-эндпоинты каталога работают, фронт может их дёргать.

Файлы:
- `catalog/Category.java`, `Book.java`
- `catalog/CategoryRepository.java`, `BookRepository.java` (+ кастомный `findByFilters(...)`)
- `catalog/dto/*` — `CategoryDto`, `BookDto`, `BookFilterRequest`
- `catalog/mapper/*` (через MapStruct, либо ручные мапперы — **выбираем ручные** для скорости и отсутствия конфигурации)
- `catalog/CategoryService.java`, `BookService.java`
- `catalog/CategoryController.java`, `BookController.java`
- Тесты: `BookServiceTest` (mock repos), `BookControllerIntegrationTest` (Testcontainers, без авторизации — GET публичный)

Шаги:
1. Entities с relationships (`@ManyToOne(fetch = LAZY)` book→category).
2. Репозитории: `BookRepository extends JpaRepository<Book, Long>` + `Page<Book> findByFilters(@Param("q") String q, @Param("categoryId") Long categoryId, @Param("min") BigDecimal min, @Param("max") BigDecimal max, Pageable pageable)` через `@Query`.
3. `BookService.search(...)` принимает фильтры + `Pageable` (парсинг `?page=&size=&sort=`).
4. Контроллеры: `@RestController @RequestMapping("/api/books") @RequiredArgsConstructor`.
5. Интеграционный тест: GET `/api/books` возвращает 6 книг из seed, GET `/api/books/search?categoryId=1` фильтрует, GET `/api/books/999` → 404.
6. `git commit -m "feat(catalog): public read endpoints for categories and books"`.

---

### День 4 — Catalog admin write + Cart
**Цель:** админ может управлять книгами/категориями; пользователь управляет корзиной.

Файлы:
- CRUD-операции в `CategoryService`/`BookService` (валидация, проверки уникальности, `existsBy` перед удалением)
- `cart/CartItem.java`, `CartItemId.java` (composite PK через `@IdClass` ИЛИ встраиваемый `@EmbeddedId` — выбираем `@IdClass` для простоты), `CartItemRepository.java`
- `cart/dto/CartDto.java`, `CartItemDto.java`, `AddToCartRequest.java`
- `cart/CartService.java`, `CartController.java`
- Тесты: админ-операции (создание/обновление/удаление книги), операции корзины (add/update/remove, пересчёт суммы)

Шаги:
1. `@PreAuthorize("hasRole('ADMIN')")` на admin-эндпоинтах (включить `@EnableMethodSecurity` в SecurityConfig).
2. Проверка в `BookService.delete(id)`: кидать 409, если есть `OrderItem` с этим `bookId`.
3. Корзина: `CartService.getCurrentCart(userId)` — если нет ни одной `CartItem` для пользователя, вернуть пустую.
4. `CartService.addItem(userId, bookId, qty)` — `findByUserIdAndBookId`, если есть — `quantity += qty`, иначе новая. Проверка `stock`.
5. Тест: пользователь A не видит корзину пользователя B; добавление сверх `stock` → 400.
6. `git commit -m "feat(catalog,cart): admin write ops and cart management"`.

---

### День 5 — Orders + транзакционная сборка
**Цель:** можно оформить заказ из корзины, история заказов работает.

Файлы:
- `order/Order.java`, `OrderItem.java`
- `order/OrderRepository.java`, `OrderItemRepository.java`
- `order/dto/OrderDto.java`, `OrderItemDto.java`, `CreateOrderRequest.java`, `UpdateStatusRequest.java`
- `order/OrderService.java`, `OrderController.java`
- `common/exception/InsufficientStockException.java` (обрабатывается в `GlobalExceptionHandler` → 409)
- Тесты: транзакционный сценарий «корзина → заказ» с проверкой списания `stock`, откат при ошибке, `status` flow

Шаги:
1. `Order` + `OrderItem` (one-to-many, каскад PERSIST, orphanRemoval).
2. `OrderService.create(userId, shippingAddress)` — `@Transactional`:
   - загрузить `cartItems` для user
   - проверить `stock` по каждой книге (с `@Lock(PESSIMISTIC_WRITE)` через `findById` для защиты от гонки — **важно для конкурентности**)
   - создать `Order` + список `OrderItem` с **замороженной ценой** (`book.getPrice()` на момент покупки)
   - уменьшить `stock` на `quantity`
   - удалить `cartItems`
   - сохранить всё одной транзакцией
3. `OrderService.cancel(orderId)` — ADMIN, возврат `stock` (если статус `NEW` или `PAID`).
4. PATCH `/api/orders/{id}/status` — ADMIN.
5. Тест: успешный заказ уменьшает stock, неуспешный (не хватает stock) — откат, корзина не тронута.
6. `git commit -m "feat(order): checkout flow with transactional stock decrement"`.

---

### День 6 — Тесты, наблюдаемость, логирование, OpenAPI
**Цель:** покрыть тестами, получить Swagger UI, настроить логи.

Файлы:
- `src/test/java/.../*IntegrationTest.java` (добить покрытие ключевых сценариев)
- `src/main/resources/logback-spring.xml` — формат с timestamp/level/logger/thread/mdc
- `application.properties` — `management.endpoints.web.exposure.include=health,info,metrics`, `logging.level.org.hibernate.SQL=DEBUG` (только dev-профиль)
- `src/main/java/.../common/logging/RequestLoggingFilter.java` — логирует входящие запросы с `X-Request-Id`
- Документация API через springdoc (если подключили) — `/swagger-ui.html`

Шаги:
1. Прогнать `./gradlew test`, добить зелёное покрытие.
2. `RequestLoggingFilter` — `OncePerRequestFilter`, читает/генерит `X-Request-Id` (UUID), кладёт в MDC, логирует `method path status durationMs`.
3. Скриншот Swagger UI для отчёта по практике.
4. `git commit -m "chore: tests, observability, request logging"`.

**Проверка:** Swagger UI открывается, в логах видны запросы с `requestId`.

---

### День 7 — Frontend setup + Auth + Layout
**Цель:** Angular проект собирается, можно зарегистрироваться/залогиниться, видны каталог и корзина (без фильтров).

Файлы (Angular):
- `bookshop-frontend/` (Angular 17, standalone components, Angular Router, Reactive Forms, HttpClient)
- `src/app/app.config.ts` — `provideRouter`, `provideHttpClient(withInterceptors([authInterceptor]))`, `provideAnimations`
- `src/app/core/auth/auth.service.ts` — `login`, `register`, `logout`, `currentUser$` (BehaviorSubject)
- `src/app/core/auth/auth.interceptor.ts` — подставляет токен
- `src/app/core/auth/auth.guard.ts`, `role.guard.ts`
- `src/app/shared/layout/header.component.ts` (логотип, поиск, юзер-меню)
- `src/app/features/auth/login.component.ts`, `register.component.ts`
- `src/app/features/catalog/catalog.component.ts` (заглушка — список книг)
- `src/app/core/api/api.service.ts` — `HttpClient` обёртки (`getBooks()`, `getCategories()`)
- `src/environments/environment.ts` — `apiUrl: 'http://localhost:8080/api'`

Шаги:
1. `npx -p @angular/cli@17 ng new bookshop-frontend --routing --style=scss --standalone --skip-git`.
2. Добавить `bootstrap`: `npm i bootstrap @popperjs/core`. Подключить стили в `angular.json`.
3. Реализовать `AuthService` с `localStorage` (ключ `bookshop_token`).
4. `authInterceptor` — functional interceptor: если есть токен — добавить header, если 401 — редирект на `/login`.
5. Страницы login/register с валидацией Reactive Forms.
6. `ng serve` — открыть `http://localhost:4200`, проверить сценарий.
7. `git commit -m "feat(frontend): angular setup, auth, basic layout"`.

**Важно:** из-за CORS backend должен отвечать `Access-Control-Allow-Origin: http://localhost:4200` (настроено в День 2). Если в браузере ошибка — вернуться и поправить.

---

### День 8 — Frontend каталог, корзина, оформление
**Цель:** полный happy-path работает end-to-end.

Файлы:
- `features/catalog/catalog.component.html` — сетка карточек, фильтр-сайдбар (категория select, цена min/max, поиск), пагинация
- `features/catalog/book-detail.component.ts`
- `features/cart/cart.component.ts` — список, кнопки +/-/удалить, «Оформить заказ»
- `features/orders/orders-list.component.ts`, `order-detail.component.ts`
- `core/api/cart.service.ts`, `order.service.ts`

Шаги:
1. Компонент каталога: `FormGroup` с фильтрами, `debounceTime(300)` на поиск, `switchMap` на запросы.
2. Карточка книги — кнопка «В корзину» (для USER; для guest — редирект на login).
3. Корзина: получение, обновление qty, удаление, расчёт суммы.
4. Оформление: форма с `shippingAddress`, POST `/api/orders`, при успехе — очистка корзины, редирект на `/orders/:id`.
5. Проверить e2e: регистрация → логин → каталог → добавить 2 книги → корзина → заказ → виден в `/orders`.
6. `git commit -m "feat(frontend): catalog, cart, checkout flow"`.

---

### День 9 — Admin-панель + Docker всего стека
**Цель:** админ может управлять книгами/категориями через UI; весь стек поднимается одной командой.

Файлы (frontend):
- `features/admin/admin-books.component.ts`, `admin-categories.component.ts` — простые таблицы с CRUD-модалками
- `core/guards/admin.guard.ts`

Файлы (backend/Docker):
- `Dockerfile` (multi-stage: `gradle:8-jdk21` build → `eclipse-temurin:21-jre`)
- `.dockerignore`
- `compose.yaml` — добавить сервис `backend` с `depends_on: postgres`, `SPRING_PROFILES_ACTIVE=prod`
- `application-prod.properties` — отключить SQL-логи

Шаги:
1. Admin-страницы — простая таблица + кнопка «Создать» открывает форму (template-driven, минимум).
2. Проверить: зайти под `admin/admin123` (из seed) → создать книгу → видна в каталоге.
3. Dockerfile: `COPY . .` → `./gradlew bootJar -x test` → `COPY build/libs/*.jar app.jar` → `ENTRYPOINT java -jar app.jar`.
4. `compose.yaml`: дополнить сервисом `backend` (build context `.`, port 8080).
5. `docker compose up -d --build`. Проверить: фронт (если запускать отдельно) → запросы идут на `http://localhost:8080`.
6. `git commit -m "feat(admin,deploy): admin ui, full docker stack"`.

---

### День 10 — Отчёт, диагностика, нагрузочный smoke-test
**Цель:** сдать практику: рабочее приложение + сопроводительные артефакты.

Что делаем:
1. **Тесты:** прогнать `./gradlew test` (backend), `ng test --watch=false` (frontend), заскринить результаты.
2. **Документация:**
   - `README.md` — как поднять (docker compose up), архитектура, как залогиниться (admin/admin123), эндпоинты.
   - `docs/db-schema.md` — ER-диаграмма (нарисовать в Draw.io, экспортировать PNG).
   - `docs/api.md` — список эндпоинтов (можно сгенерировать через springdoc OpenAPI → /v3/api-docs).
3. **Диагностика:** пример логов при сценарии «заказ без stock» + как смотреть через `docker compose logs backend | grep ERROR`.
4. **Нагрузочный smoke:** через `curl` в цикле `for i in $(seq 1 50); do curl -s http://localhost:8080/api/books >/dev/null; done` — замерить время, проверить `docker stats`, что нет утечек памяти.
5. **Заполнить отчёт** по компетенциям (таблица из ТЗ) — какие задачи покрывают какие ПК.
6. `git commit -m "docs: report, schema, api reference"`.

**Проверка финальная:** `docker compose down -v && docker compose up -d --build && curl http://localhost:8080/actuator/health` — работает. Этот сценарий записать в README.

---

## 6. Покрытие компетенций (маппинг задач → ПК)

| Компетенция | Что покрывает | Где в плане |
|---|---|---|
| **ПК-2.2** Разработка тестов | unit + integration тесты | День 2 (Auth), 3 (Catalog), 4 (Cart), 5 (Order), 6 (полное покрытие) |
| **ПК-3.1** Процедура интеграции, обоснование технологий | День 7–9 интеграция, раздел 1 (архитектура) + 6 (выбор стека в отчёте) | |
| **ПК-3.2** Интеграция модулей, верификация | end-to-end проверка День 8 + 10 | |
| **ПК-4.1** Настройка сетевых элементов | CORS, порты 8080/4200/5432, `compose.yaml` | День 2 (CORS), 9 (Docker network) |
| **ПК-4.2** Контроль ресурсов, диагностика | `docker stats`, `RequestLoggingFilter`, логи, `actuator/health` | День 6 + 10 |
| **ПК-5.2** Инструментальное средство программирования | свой `JwtService`, `RequestLoggingFilter` (минимальный) | День 2, 6 |
| **ПК-6.1** Инструментальные средства проектирования | Flyway, springdoc, Postman, IntelliJ | День 1, 6 |
| **ПК-6.2** Анализ требований, проектирование | День 1 — раздел 2 (ER) | |
| **ПК-7.3** Прототип ИС | HTML/Bootstrap-прототип (по желанию) или Angular UI | День 7–9 |
| **ПК-7.5** Логическая и физическая модели БД | Раздел 2 + `docs/db-schema.md` | |
| **ПК-7.6** Сборка конфигурации ИС | Docker multi-stage, compose.yaml, профили | День 9 |

---

## 7. Технологический стек — обоснование (кратко, для отчёта)

| Решение | Почему |
|---|---|
| **Spring Boot 4.1** | Уже в проекте; модуль security+JPA+web из коробки. |
| **Java 21** | LTS, уже в toolchain. |
| **JPA + Hibernate** | Стандарт для реляционных БД на JVM; меньше boilerplate, чем JDBC. |
| **PostgreSQL 16** | Требование ТЗ; поддержка `NUMERIC`, `JSONB` (на будущее). |
| **Flyway** | Миграции как код; воспроизводимая БД; в отчёте — соответствие «проектирование БД». |
| **JWT (jjwt)** | Stateless-аутентификация для SPA; не нужен сервер сессий. |
| **BCrypt** | Индустриальный стандарт хеширования паролей (требование ТЗ). |
| **Angular 17 standalone** | Без NgModules — меньше церемоний; onPush + signals дают простую реактивность. |
| **Bootstrap 5** | Быстрая вёрстка; не тянет тяжёлый Material в bundle. |
| **Docker Compose** | Одна команда для развёртывания всего стека (ПК-7.6). |
| **Testcontainers** | Реальная PostgreSQL в тестах — не мокаем БД. |

Альтернативы, которые **рассмотрели и отклонили**:
- *Spring Modulith* (уже подключён) — переусложнение для 10 дней; оставляем зависимость, но не используем.
- *GraphQL* — для книжного магазина REST достаточно, GraphQL добавляет 1–2 дня.
- *Keycloak* для auth — избыточно, свой JWT-фильтр за полдня.
- *MapStruct* — ручные мапперы читаемее на малом числе полей.

---

## 8. Принятые решения и дефолты

**Решено пользователем:**
1. ✅ **Spring Modulith** — оставляем как есть, не используем фичи Modulith.
2. ✅ **UI-kit** — Bootstrap 5.
3. ✅ **Обложки книг** — локальные файлы в `uploads/covers/`, в БД поле `cover_url VARCHAR(512)` (Java `String`) с путём `/uploads/covers/{uuid}.jpg`. Seed-картинки лежат в `src/main/resources/static/seed-covers/`, копируются в `uploads/` при первом старте (`@PostConstruct` в `CoverService`).

**Дефолт (можно изменить при старте):**
4. **Объём тестов** — полное покрытие: unit на сервисы + integration с Testcontainers на ключевые сценарии (auth, catalog, cart, order). Это даёт ~15–20 тестов и твёрдо покрывает ПК-2.2. Если времени не хватит — приоритет у `OrderServiceIntegrationTest` (там самая нетривиальная логика — транзакция, списание `stock`, заморозка цены).
5. **Пагинация** — Spring Data `Pageable` на backend, на frontend — простая кнопка «Дальше / Назад».

---

## 9. Куда сохранить итоговый план

В режиме плана разрешено редактировать только файл `C:\Users\sotni\.claude\plans\1-ethereal-bonbon.md`. После одобрения плана и выхода из режима плана я:
1. Скопирую содержимое в `C:\Spring\BookShop\plan.md` (как просил пользователь).
2. Начну реализацию согласно Дню 1.
