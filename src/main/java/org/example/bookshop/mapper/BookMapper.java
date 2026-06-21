package org.example.bookshop.mapper;

import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookMapper {

    @Mapping(target = "categoryId",
        expression = "java(book.getCategory() != null ? book.getCategory().getId() : null)")
    @Mapping(target = "categoryName",
        expression = "java(book.getCategory() != null ? book.getCategory().getName() : null)")
    BookDto toDto(Book book);
}