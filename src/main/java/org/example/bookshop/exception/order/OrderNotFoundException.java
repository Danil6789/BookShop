package org.example.bookshop.exception.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Заказ с id=" + id + " не найден");
    }
}
