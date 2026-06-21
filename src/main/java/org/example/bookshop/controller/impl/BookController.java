package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookshop.controller.BookApi;
import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.dto.catalog.BookFilterRequest;
import org.example.bookshop.dto.catalog.BookRequest;
import org.example.bookshop.dto.catalog.PageResponse;
import org.example.bookshop.service.catalog.BookService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookController implements BookApi {

    private final BookService service;

    @Override
    public ResponseEntity<PageResponse<BookDto>> findAll(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(service.search(new BookFilterRequest(), pageable));
    }

    @Override
    public ResponseEntity<PageResponse<BookDto>> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        Pageable pageable
    ) {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setQ(q);
        filter.setCategoryId(categoryId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        return ResponseEntity.ok(service.search(filter, pageable));
    }

    @Override
    public ResponseEntity<BookDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Override
    public ResponseEntity<BookDto> create(BookRequest request) {
        BookDto created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<BookDto> update(Long id, BookRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
