package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.controller.CategoryApi;
import org.example.bookshop.dto.catalog.CategoryDto;
import org.example.bookshop.service.catalog.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryApi {

    private final CategoryService service;

    @Override
    public ResponseEntity<List<CategoryDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
