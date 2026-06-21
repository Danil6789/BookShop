package org.example.bookshop.exception.catalog;

public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException(String name) {
        super("Категория с именем '" + name + "' уже существует");
    }
}
