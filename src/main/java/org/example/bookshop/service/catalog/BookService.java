package org.example.bookshop.service.catalog;

import jakarta.persistence.criteria.Predicate;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

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

        Specification<Book> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (filter.getQ() != null && !filter.getQ().isBlank()) {
                String pattern = "%" + filter.getQ().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(cb.ge(root.<BigDecimal>get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.le(root.<BigDecimal>get("price"), filter.getMaxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Book> page = repository.findAll(spec, pageable);
        return PageResponse.from(page, BookMapper::toDto);
    }
}
