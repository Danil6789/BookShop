package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.dto.catalog.BookRequest;
import org.example.bookshop.dto.catalog.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

import static org.example.bookshop.constant.ApiPath.BOOKS_BY_ID_URL;
import static org.example.bookshop.constant.ApiPath.BOOKS_SEARCH_URL;
import static org.example.bookshop.constant.ApiPath.BOOKS_URL;

@Tag(name = "Книги", description = "Публичный API для каталога книг (admin — POST/PUT/DELETE)")
@RequestMapping(BOOKS_URL)
public interface BookApi {

    @GetMapping
    @Operation(
        summary = "Список книг (пагинированный)",
        description = "Возвращает все книги. Поддерживает пагинацию через ?page=&size=&sort= (по умолчанию sort=id,asc)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Страница книг")
    })
    ResponseEntity<PageResponse<BookDto>> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable);

    @GetMapping(BOOKS_SEARCH_URL)
    @Operation(
        summary = "Поиск книг с фильтрами",
        description = "Фильтры: q (поиск по названию, регистронезависимый), categoryId, minPrice, maxPrice. " +
            "Все фильтры опциональны. Конфликтные диапазоны (minPrice > maxPrice) возвращают пустой результат."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Страница книг, удовлетворяющих фильтрам")
    })
    ResponseEntity<PageResponse<BookDto>> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        Pageable pageable
    );

    @GetMapping(BOOKS_BY_ID_URL)
    @Operation(summary = "Получить книгу по id", description = "Возвращает книгу с категорией")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Книга найдена"),
        @ApiResponse(responseCode = "404", description = "Книга не найдена")
    })
    ResponseEntity<BookDto> findById(@PathVariable Long id);

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать книгу (ADMIN)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Книга создана"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав ADMIN"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    ResponseEntity<BookDto> create(@Valid @RequestBody BookRequest request);

    @PutMapping(BOOKS_BY_ID_URL)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить книгу (ADMIN)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Книга обновлена"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав ADMIN"),
        @ApiResponse(responseCode = "404", description = "Книга или категория не найдены")
    })
    ResponseEntity<BookDto> update(@PathVariable Long id, @Valid @RequestBody BookRequest request);

    @DeleteMapping(BOOKS_BY_ID_URL)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить книгу (ADMIN)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Книга удалена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав ADMIN"),
        @ApiResponse(responseCode = "404", description = "Книга не найдена"),
        @ApiResponse(responseCode = "409", description = "Книга присутствует в заказах")
    })
    ResponseEntity<Void> delete(@PathVariable Long id);
}
