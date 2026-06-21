package org.example.bookshop.exception.catalog;

public class BookInActiveOrdersException extends RuntimeException {
    public BookInActiveOrdersException(Long bookId) {
        super("Невозможно удалить книгу с id=" + bookId + ": она присутствует в заказах");
    }
}
