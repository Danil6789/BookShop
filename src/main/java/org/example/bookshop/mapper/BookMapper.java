package org.example.bookshop.mapper;

import org.example.bookshop.dto.catalog.BookDto;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.Category;

public final class BookMapper {

    private BookMapper() {
    }

    public static BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }
        Category category = book.getCategory();
        Long categoryId = (category != null) ? category.getId() : null;
        String categoryName = (category != null) ? category.getName() : null;
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getPrice(),
                book.getStock(),
                book.getCoverUrl(),
                categoryId,
                categoryName
        );
    }
}