---
paths: "src/main/java/**/entity/**/*.java, src/main/java/**/domain/**/*.java, src/main/java/**/user/**/*.java, src/main/java/**/catalog/**/*.java, src/main/java/**/cart/**/*.java, src/main/java/**/order/**/*.java"
---

# JPA Entity Patterns

Применяется ко всем `@Entity` классам. Если ты добавляешь/меняешь сущность — **соблюдай эти правила**.

## Lombok на классе

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "books")
public class Book { ... }
```

## ID — BIGSERIAL

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
@EqualsAndHashCode.Include
private Long id;
```

## Equals/HashCode — только по id

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Book {
    @EqualsAndHashCode.Include
    private Long id;
}
```

Отношения автоматически исключаются → не ссоримся с lazy-load.

## toString — без lazy-отношений

```java
@ToString
public class Book {
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Category category;
}
```

Без `@ToString.Exclude` Hibernate дёрнет lazy-load при логировании.

## BigDecimal — precision/scale на колонке

```java
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal price;
```

`precision = 10, scale = 2` — для всех денег.

## Relations — LAZY + явный @JoinColumn

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id")
private Category category;
```

Не пиши `fetch = EAGER` без явной причины.

## Enums — STRING

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Role role;  // USER / ADMIN
```

Никогда не `EnumType.ORDINAL` — ломается при перестановке enum-значений.

## Defaults — @PrePersist

```java
@PrePersist
void onCreate() {
    if (role == null) role = Role.USER;
    if (status == null) status = Status.PENDING;
}
```

На случай если поле не задано вручную (например, через DTO без role).

## UNIQUE constraints

```java
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"})
)
public class CartItem { ... }
```

Источник истины — Flyway-миграция. `@UniqueConstraint` в `@Table` — для валидации Hibernate.

## Hibernate режим

- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate только проверяет, что entity совпадает со схемой Flyway.
- `spring.jpa.open-in-view: false` — никаких lazy-relations в контроллерах.
