package org.example.bookshop.service.catalog;

import org.example.bookshop.dto.catalog.CategoryDto;
import org.example.bookshop.dto.catalog.CategoryRequest;
import org.example.bookshop.entity.Category;
import org.example.bookshop.exception.catalog.CategoryAlreadyExistsException;
import org.example.bookshop.exception.catalog.CategoryHasBooksException;
import org.example.bookshop.exception.catalog.CategoryNotFoundException;
import org.example.bookshop.mapper.CategoryMapper;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CategoryRepository;
import org.example.bookshop.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private BookRepository bookRepository;
    @Mock private OrderItemRepository orderItemRepository;

    private final CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, bookRepository, orderItemRepository, categoryMapper);
    }

    @Test
    void create_newName_savesAndReturnsDto() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Поэзия");
        Category saved = Category.builder().id(10L).name("Поэзия").build();
        when(categoryRepository.existsByName("Поэзия")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryDto result = service.create(req);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Поэзия");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_duplicateName_throwsCategoryAlreadyExists() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Детектив");
        when(categoryRepository.existsByName("Детектив")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasMessageContaining("Детектив");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_existingCategory_updatesName() {
        Category existing = Category.builder().id(1L).name("Старое имя").build();
        CategoryRequest req = new CategoryRequest();
        req.setName("Новое имя");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Новое имя")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDto result = service.update(1L, req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Новое имя");
    }

    @Test
    void update_nameTakenByOtherCategory_throws() {
        Category existing = Category.builder().id(1L).name("Старое").build();
        CategoryRequest req = new CategoryRequest();
        req.setName("Занято");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Занято")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, req))
            .isInstanceOf(CategoryAlreadyExistsException.class);
    }

    @Test
    void update_sameName_doesNotThrowConflict() {
        Category existing = Category.builder().id(1L).name("То же").build();
        CategoryRequest req = new CategoryRequest();
        req.setName("То же");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDto result = service.update(1L, req);

        assertThat(result.getName()).isEqualTo("То же");
    }

    @Test
    void update_missingCategory_throwsCategoryNotFound() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Любое");
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, req))
            .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void delete_emptyCategory_deletesIt() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsByCategory_Id(1L)).thenReturn(false);

        service.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void delete_categoryWithBooks_throwsCategoryHasBooks() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsByCategory_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf(CategoryHasBooksException.class);

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_missingCategory_throwsCategoryNotFound() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).deleteById(any());
    }
}
