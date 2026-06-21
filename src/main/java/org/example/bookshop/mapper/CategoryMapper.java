package org.example.bookshop.mapper;

import org.example.bookshop.dto.catalog.CategoryDto;
import org.example.bookshop.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    CategoryDto toDto(Category category);
}