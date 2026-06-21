package org.example.bookshop.exception.order;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long bookId, Integer requested, Integer available) {
        super("Недостаточно товара для книги с id=" + bookId
            + ": запрошено " + requested + ", доступно " + available);
    }
}
