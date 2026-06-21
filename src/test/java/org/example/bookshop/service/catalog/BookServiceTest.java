package org.example.bookshop.service.catalog;

import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.dto.catalog.BookFilterRequest;
import org.example.bookshop.dto.catalog.PageResponse;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.Category;
import org.example.bookshop.exception.catalog.BookNotFoundException;
import org.example.bookshop.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository repository;
    @InjectMocks private BookService service;

    @Test
    void findById_existingBook_returnsDto() {
        Book book = bookWithCategory(1L, "Мастер и Маргарита", new BigDecimal("650.00"), 1L, "Художественная литература");
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        BookDto result = service.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Мастер и Маргарита");
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getCategoryName()).isEqualTo("Художественная литература");
    }

    @Test
    void findById_missingBook_throwsBookNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_callsRepositoryWithSpecification() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setQ("Мастер");
        filter.setCategoryId(2L);
        filter.setMinPrice(new BigDecimal("500.00"));
        filter.setMaxPrice(new BigDecimal("1000.00"));
        Pageable pageable = PageRequest.of(0, 20);

        Book book = bookWithCategory(1L, "Мастер и Маргарита", new BigDecimal("650.00"), 1L, "Художественная литература");
        Page<Book> page = new PageImpl<>(List.of(book), pageable, 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<BookDto> result = service.search(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Мастер и Маргарита");
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_emptyFilter_returnsAllBooks() {
        BookFilterRequest filter = new BookFilterRequest();
        Pageable pageable = PageRequest.of(0, 20);

        Book b1 = bookWithCategory(1L, "Book 1", new BigDecimal("100"), 1L, "Cat 1");
        Book b2 = bookWithCategory(2L, "Book 2", new BigDecimal("200"), 1L, "Cat 1");
        Page<Book> page = new PageImpl<>(List.of(b1, b2), pageable, 2);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<BookDto> result = service.search(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    private static Book bookWithCategory(Long id, String title, BigDecimal price, Long catId, String catName) {
        Category category = Category.builder().id(catId).name(catName).build();
        return Book.builder()
            .id(id)
            .title(title)
            .price(price)
            .stock(10)
            .category(category)
            .build();
    }
}
