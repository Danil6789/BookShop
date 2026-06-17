# 📚 BookShop

Простой книжный магазин — учебный проект (производственная практика).

Полноценное клиент-серверное приложение с регистрацией, каталогом книг,
корзиной и оформлением заказов.

## 🚀 Стек

- **Frontend:** Angular 17
- **Backend:** Java 17 + Spring Boot 3.2 (Spring Security + JWT)
- **Database:** PostgreSQL 15
- **Тесты:** JUnit 5 + Mockito

## 📦 Структура проекта

```
.
├── db/                       SQL-скрипты для PostgreSQL
│   ├── 00_create_database.sql    создать БД и пользователя
│   ├── 01_schema.sql             создать таблицы
│   └── 02_seed_data.sql          тестовые данные
├── backend/                  Spring Boot приложение
│   ├── src/main/java/ru/bookshop/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── db/schema.sql
│   │   ├── db/data.sql
│   │   └── static/covers/        обложки книг
│   └── pom.xml
└── frontend/                 Angular приложение
    └── src/app/
```

## 🗄️ База данных (6 таблиц)

| Таблица | Назначение |
|---|---|
| `users` | пользователи (USER / ADMIN) |
| `categories` | жанры книг |
| `books` | книги с обложками |
| `cart_items` | корзина |
| `orders` | заказы |
| `order_items` | позиции в заказе |

## ⚡ Быстрый старт

### 1. Поднять PostgreSQL

```bash
# через Docker
docker run -d --name bookshop-db \
  -e POSTGRES_USER=bookshop \
  -e POSTGRES_PASSWORD=bookshop \
  -e POSTGRES_DB=bookshop \
  -p 5432:5432 \
  postgres:15
```

### 2. Залить схему и данные

```bash
psql -U bookshop -d bookshop -f db/01_schema.sql
psql -U bookshop -d bookshop -f db/02_seed_data.sql
```

### 3. Запустить бэкенд

```bash
cd backend
./mvnw spring-boot:run
# стартует на http://localhost:8080
```

### 4. Запустить фронтенд

```bash
cd frontend
npm install
npm start
# стартует на http://localhost:4200
```

## 👤 Тестовые пользователи

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `password` | ADMIN |
| `ivan` | `password` | USER |

## 🔌 REST API

### Аутентификация
- `POST /api/auth/register` — регистрация
- `POST /api/auth/login` — вход

### Книги
- `GET /api/books?search=&categoryId=` — поиск/фильтр
- `GET /api/books/{id}` — карточка книги
- `POST /api/books` — создать *(ADMIN)*
- `PUT /api/books/{id}` — обновить *(ADMIN)*
- `DELETE /api/books/{id}` — удалить *(ADMIN)*

### Категории
- `GET /api/categories` — список
- `POST /api/categories` — создать *(ADMIN)*

### Корзина (требуется JWT)
- `GET /api/cart` — посмотреть
- `POST /api/cart` — добавить книгу
- `PUT /api/cart/{itemId}` — изменить количество
- `DELETE /api/cart/{itemId}` — удалить позицию
- `DELETE /api/cart` — очистить

### Заказы (требуется JWT)
- `POST /api/orders/checkout` — оформить заказ из корзины
- `GET /api/orders/my` — мои заказы
- `GET /api/orders/{id}` — посмотреть заказ

### Прочее
- `GET /api/health` — проверка живости сервиса

## ✅ Тесты

```bash
cd backend
./mvnw test
```

Покрытие: AuthService (регистрация, логин), BookService (CRUD, ошибки).

## 📝 Лицензия

Учебный проект. Используй как хочешь.
