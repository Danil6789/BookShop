package org.example.bookshop.exception.catalog;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(Long id) {
        super("Книга с id=" + id + " не найдена");
    }
}
