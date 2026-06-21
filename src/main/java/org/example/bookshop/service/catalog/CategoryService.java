package org.example.bookshop.service.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;
    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryDto> findAll() {
        return repository.findAll(Sort.by("name")).stream()
            .map(categoryMapper::toDto)
            .toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest request) {
        log.debug("Creating category with name='{}'", request.getName());
        if (repository.existsByName(request.getName())) {
            throw new CategoryAlreadyExistsException(request.getName());
        }
        Category category = Category.builder()
            .name(request.getName())
            .build();
        Category saved = repository.save(category);
        log.info("Created category id={}, name='{}'", saved.getId(), saved.getName());
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        log.debug("Updating category id={}, new name='{}'", id, request.getName());
        Category category = repository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException(id));

        if (!category.getName().equals(request.getName()) && repository.existsByName(request.getName())) {
            throw new CategoryAlreadyExistsException(request.getName());
        }

        category.setName(request.getName());
        Category saved = repository.save(category);
        log.info("Updated category id={}, name='{}'", saved.getId(), saved.getName());
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting category id={}", id);
        if (!repository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        if (bookRepository.existsByCategory_Id(id)) {
            throw new CategoryHasBooksException(id);
        }
        repository.deleteById(id);
        log.info("Deleted category id={}", id);
    }
}
