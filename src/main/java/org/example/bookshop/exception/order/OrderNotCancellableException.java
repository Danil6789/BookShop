package org.example.bookshop.exception.order;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(Long id, String status) {
        super("Заказ с id=" + id + " нельзя отменить в статусе " + status);
    }
}
