package org.example.bookshop.service.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.dto.catalog.BookFilterRequest;
import org.example.bookshop.dto.catalog.PageResponse;
import org.example.bookshop.entity.Book;
import org.example.bookshop.exception.catalog.BookNotFoundException;
import org.example.bookshop.mapper.BookMapper;
import org.example.bookshop.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository repository;

    public BookDto findById(Long id) {
        Book book = repository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
        return BookMapper.toDto(book);
    }

    public PageResponse<BookDto> search(BookFilterRequest filter, Pageable pageable) {
        log.debug("Searching books with filter={}, pageable={}", filter, pageable);
        Page<Book> page = repository.search(
            filter.getQ(),
            filter.getCategoryId(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            pageable
        );
        return PageResponse.from(page, BookMapper::toDto);
    }
}
