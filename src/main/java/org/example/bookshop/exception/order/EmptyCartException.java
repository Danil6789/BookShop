package org.example.bookshop.exception.order;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException() {
        super("Корзина пуста — невозможно оформить заказ");
    }
}
