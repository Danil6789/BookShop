package org.example.bookshop.exception.catalog;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(Long id) {
        super("Категория с id=" + id + " не найдена");
    }
}
