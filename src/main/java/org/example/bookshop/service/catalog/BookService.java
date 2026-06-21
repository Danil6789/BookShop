package org.example.bookshop.service.catalog;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.dto.catalog.BookFilterRequest;
import org.example.bookshop.dto.catalog.BookRequest;
import org.example.bookshop.dto.catalog.PageResponse;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.Category;
import org.example.bookshop.exception.catalog.BookInActiveOrdersException;
import org.example.bookshop.exception.catalog.BookNotFoundException;
import org.example.bookshop.exception.catalog.CategoryNotFoundException;
import org.example.bookshop.mapper.BookMapper;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CategoryRepository;
import org.example.bookshop.repository.OrderItemRepository;
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

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookMapper bookMapper;

    public BookDto findById(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
        return bookMapper.toDto(book);
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

        Page<Book> page = bookRepository.findAll(spec, pageable);
        return PageResponse.from(page, bookMapper::toDto);
    }

    @Transactional
    public BookDto create(BookRequest request) {
        log.debug("Creating book with title='{}'", request.getTitle());
        Category category = resolveCategory(request.getCategoryId());
        Book book = Book.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .price(request.getPrice())
            .stock(request.getStock())
            .coverUrl(request.getCoverUrl())
            .category(category)
            .build();
        Book saved = bookRepository.save(book);
        log.info("Created book id={}, title='{}'", saved.getId(), saved.getTitle());
        return bookMapper.toDto(saved);
    }

    @Transactional
    public BookDto update(Long id, BookRequest request) {
        log.debug("Updating book id={}, new title='{}'", id, request.getTitle());
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));

        Category category = resolveCategory(request.getCategoryId());

        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setCoverUrl(request.getCoverUrl());
        book.setCategory(category);

        Book saved = bookRepository.save(book);
        log.info("Updated book id={}, title='{}'", saved.getId(), saved.getTitle());
        return bookMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting book id={}", id);
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        if (orderItemRepository.existsByBookId(id)) {
            throw new BookInActiveOrdersException(id);
        }
        bookRepository.deleteById(id);
        log.info("Deleted book id={}", id);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
