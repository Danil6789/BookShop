package org.example.bookshop.exception.order;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException() {
        super("Нет доступа к чужому заказу");
    }
}
