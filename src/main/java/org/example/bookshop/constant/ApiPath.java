package org.example.bookshop.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPath {
    public static final String AUTH_REGISTER_URL = "/register";
    public static final String AUTH_LOGIN_URL = "/login";

    public static final String CATEGORIES_URL = "/api/categories";
    public static final String CATEGORIES_BY_ID_URL = "/{id}";
    public static final String BOOKS_URL = "/api/books";
    public static final String BOOKS_BY_ID_URL = "/{id}";
    public static final String BOOKS_SEARCH_URL = "/search";

    public static final String CART_URL = "/api/cart";
    public static final String CART_ITEMS_URL = "/items";
    public static final String CART_ITEM_BY_BOOK_ID_URL = "/items/{bookId}";

    public static final String ORDERS_URL = "/api/orders";
}
