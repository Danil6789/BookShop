package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.bookshop.dto.catalog.CategoryDto;
import org.example.bookshop.dto.catalog.CategoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.example.bookshop.constant.ApiPath.CATEGORIES_BY_ID_URL;
import static org.example.bookshop.constant.ApiPath.CATEGORIES_URL;

@Tag(name = "Категории", description = "Публичный API для категорий книг (admin — POST/PUT/DELETE)")
@RequestMapping(CATEGORIES_URL)
public interface CategoryApi {

    @GetMapping
    @Operation(summary = "Список всех категорий", description = "Возвращает все категории, отсортированные по имени")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список категорий")
    })
    ResponseEntity<List<CategoryDto>> findAll();

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать категорию (ADMIN)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Категория создана"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав ADMIN"),
        @ApiResponse(responseCode = "409", description = "Категория с таким именем уже существует")
    })
    ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest request);

    @PutMapping(CATEGORIES_BY_ID_URL)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить категорию (ADMIN)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Категория обновлена"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав ADMIN"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "409", description = "Категория с таким именем уже существует")
    })
    ResponseEntity<CategoryDto> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request);

    @DeleteMapping(CATEGORIES_BY_ID_URL)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить категорию (ADMIN)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Категория удалена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав ADMIN"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "409", description = "Категория содержит книги")
    })
    ResponseEntity<Void> delete(@PathVariable Long id);
}
