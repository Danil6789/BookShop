---
paths: "src/main/java/**/config/security/**/*.java, src/main/java/**/config/swagger/**/*.java, src/main/java/**/service/auth/**/*.java, src/main/java/**/controller/**/Auth*.java, src/main/java/**/controller/impl/AuthController.java"
---

# Spring Security + JWT Conventions

Применяется ко всему что касается auth, JWT, SecurityConfig, Swagger UI.

## Critical invariant: ROLE_ prefix

**`JwtService.generate()`** хранит role **БЕЗ** `ROLE_` префикса:

```java
.claim("role", role)  // role = "USER" или "ADMIN", без префикса
```

**`JwtAuthenticationFilter`** сам добавляет `ROLE_` при создании `SimpleGrantedAuthority`:

```java
new SimpleGrantedAuthority("ROLE_" + role)
```

**`UserDetailsImpl.getAuthorities()`** тоже возвращает `["ROLE_" + role]`.

Это значит: `.hasRole("ADMIN")` в SecurityConfig работает корректно. **Не ломай этот контракт** — не пиши `ROLE_` в `JwtService` (будет `ROLE_ROLE_ADMIN`).

## SecurityConfig rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/auth/**", "/actuator/health",
        "/v3/api-docs/**", "/v3/api-docs", "/v3/api-docs.yaml",
        "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**"
    ).permitAll()
    .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
    .anyRequest().authenticated()
)
```

При добавлении новых публичных путей — добавляй в `permitAll()`. Иначе получишь 403.

## JWT structure

- Algorithm: HS256
- Secret: ≥32 bytes (валидируется в конструкторе `JwtService`)
- Claims: `sub` (username), `role`, `iat`, `exp`, `iss`
- Expiration: 24h по умолчанию (`app.jwt.expiration-hours`)

## BCrypt

- Strength: 10 (в `SecurityConfig.passwordEncoder()`)
- Не менять без явной причины

## Swagger UI

- Path: `/swagger-ui/index.html`
- `OpenApiConfig` в `config/swagger/` — bean `OpenAPI` с `BearerAuth` SecurityScheme
- Используй `@Tag`, `@Operation`, `@ApiResponses` на `*Api` интерфейсах (не на impl)
- `springdoc.swagger-ui.persist-authorization: true` — токен не теряется при reload UI

## CORS

- Allowed origin: `http://localhost:4200` (Angular dev-сервер)
- Methods: `GET, POST, PUT, DELETE, PATCH, OPTIONS`
- `allowCredentials: true` (для будущей JWT-аутентификации из браузера)

## Test users (после Flyway V3)

- `admin` / `password` (ADMIN)
- `ivan` / `password` (USER)

V2 имел сломанный bcrypt-хеш, V3 исправил. Не удаляй V3 — seed-юзеры перестанут логиниться.

## Api+Impl split для контроллеров

```java
// controller/AuthApi.java — интерфейс с @RequestMapping + Swagger аннотациями
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация")
public interface AuthApi {
    @PostMapping("/register")
    @Operation(summary = "Регистрация")
    ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest req);
}

// controller/impl/AuthController.java — @RestController implements AuthApi
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi { ... }
```

`@RequestMapping` дублируется на обоих — это правильно, чтобы Swagger видел путь.
