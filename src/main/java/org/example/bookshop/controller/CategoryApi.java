package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.bookshop.dto.catalog.CategoryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.example.bookshop.constant.ApiPath.CATEGORIES_URL;

@Tag(name = "Категории", description = "Публичный API для категорий книг")
@RequestMapping(CATEGORIES_URL)
public interface CategoryApi {

    @GetMapping
    @Operation(summary = "Список всех категорий", description = "Возвращает все категории, отсортированные по имени")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список категорий")
    })
    ResponseEntity<List<CategoryDto>> findAll();
}
