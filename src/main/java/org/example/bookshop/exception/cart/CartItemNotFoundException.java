package org.example.bookshop.exception.cart;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long bookId) {
        super("Позиция с bookId=" + bookId + " не найдена в корзине");
    }
}