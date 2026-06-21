package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.controller.BookApi;
import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.dto.catalog.BookFilterRequest;
import org.example.bookshop.dto.catalog.PageResponse;
import org.example.bookshop.service.catalog.BookService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookController implements BookApi {

    private final BookService service;

    @Override
    public ResponseEntity<PageResponse<BookDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.search(new BookFilterRequest(), pageable));
    }

    @Override
    public ResponseEntity<PageResponse<BookDto>> search(BookFilterRequest filter, Pageable pageable) {
        return ResponseEntity.ok(service.search(filter, pageable));
    }

    @Override
    public ResponseEntity<BookDto> findById(Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
