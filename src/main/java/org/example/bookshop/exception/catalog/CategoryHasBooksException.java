package org.example.bookshop.exception.catalog;

public class CategoryHasBooksException extends RuntimeException {
    public CategoryHasBooksException(Long categoryId) {
        super("Невозможно удалить категорию с id=" + categoryId + ": она содержит книги");
    }
}
