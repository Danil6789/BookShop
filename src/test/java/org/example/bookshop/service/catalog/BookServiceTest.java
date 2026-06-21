package org.example.bookshop.service.catalog;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository repository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrderItemRepository orderItemRepository;
    private final BookMapper bookMapper = Mappers.getMapper(BookMapper.class);
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(repository, categoryRepository, orderItemRepository, bookMapper);
    }

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
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("Художественная литература");
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
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("Cat 1");
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

    @Test
    void create_withValidData_savesAndReturnsDto() {
        BookRequest req = new BookRequest();
        req.setTitle("Новая");
        req.setPrice(new BigDecimal("100.00"));
        req.setStock(10);
        req.setCategoryId(1L);

        Category cat = Category.builder().id(1L).name("Художественная литература").build();
        Book saved = Book.builder().id(99L).title("Новая").price(new BigDecimal("100.00")).stock(10).category(cat).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(repository.save(any(Book.class))).thenReturn(saved);

        BookDto result = service.create(req);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getTitle()).isEqualTo("Новая");
        assertThat(result.getCategoryId()).isEqualTo(1L);
    }

    @Test
    void create_withNullCategory_savesWithoutCategory() {
        BookRequest req = new BookRequest();
        req.setTitle("Без категории");
        req.setPrice(new BigDecimal("50.00"));
        req.setStock(5);
        Book saved = Book.builder().id(100L).title("Без категории").price(new BigDecimal("50.00")).stock(5).build();
        when(repository.save(any(Book.class))).thenReturn(saved);

        BookDto result = service.create(req);

        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    void create_withMissingCategory_throwsCategoryNotFound() {
        BookRequest req = new BookRequest();
        req.setTitle("X");
        req.setPrice(new BigDecimal("10.00"));
        req.setStock(1);
        req.setCategoryId(99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_existingBook_updatesFields() {
        Book existing = Book.builder().id(1L).title("Старое").price(new BigDecimal("100")).stock(5).build();
        BookRequest req = new BookRequest();
        req.setTitle("Новое");
        req.setPrice(new BigDecimal("200.00"));
        req.setStock(15);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookDto result = service.update(1L, req);

        assertThat(result.getTitle()).isEqualTo("Новое");
        assertThat(result.getPrice()).isEqualByComparingTo("200.00");
        assertThat(result.getStock()).isEqualTo(15);
    }

    @Test
    void update_changeCategory_updatesRelation() {
        Book existing = Book.builder().id(1L).title("Книга").price(new BigDecimal("100")).stock(5)
            .category(Category.builder().id(1L).name("Старая").build()).build();
        BookRequest req = new BookRequest();
        req.setTitle("Книга");
        req.setPrice(new BigDecimal("100"));
        req.setStock(5);
        req.setCategoryId(2L);
        Category newCat = Category.builder().id(2L).name("Новая").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookDto result = service.update(1L, req);

        assertThat(result.getCategoryId()).isEqualTo(2L);
        assertThat(result.getCategoryName()).isEqualTo("Новая");
    }

    @Test
    void update_removeCategory_setsNull() {
        Book existing = Book.builder().id(1L).title("Книга").price(new BigDecimal("100")).stock(5)
            .category(Category.builder().id(1L).name("X").build()).build();
        BookRequest req = new BookRequest();
        req.setTitle("Книга");
        req.setPrice(new BigDecimal("100"));
        req.setStock(5);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookDto result = service.update(1L, req);

        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    void update_missingBook_throwsBookNotFound() {
        BookRequest req = new BookRequest();
        req.setTitle("X");
        req.setPrice(new BigDecimal("10"));
        req.setStock(1);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, req))
            .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void delete_bookNotInOrders_deletesIt() {
        when(repository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.existsByBookId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_bookInOrders_throwsBookInActiveOrders() {
        when(repository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.existsByBookId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf(BookInActiveOrdersException.class);

        verify(repository, never()).deleteById(any());
    }
}