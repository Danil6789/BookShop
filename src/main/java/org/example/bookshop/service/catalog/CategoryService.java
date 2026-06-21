package org.example.bookshop.service.catalog;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.dto.catalog.CategoryDto;
import org.example.bookshop.mapper.CategoryMapper;
import org.example.bookshop.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper categoryMapper;

    public List<CategoryDto> findAll() {
        return repository.findAll(Sort.by("name")).stream()
            .map(categoryMapper::toDto)
            .toList();
    }
}
