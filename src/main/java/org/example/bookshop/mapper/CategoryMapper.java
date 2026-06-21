package org.example.bookshop.mapper;

import org.example.bookshop.dto.catalog.CategoryDto;
import org.example.bookshop.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }
}