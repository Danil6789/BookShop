package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.controller.CartApi;
import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.dto.cart.CartDto;
import org.example.bookshop.dto.cart.CartItemDto;
import org.example.bookshop.dto.cart.UpdateCartItemRequest;
import org.example.bookshop.entity.User;
import org.example.bookshop.service.auth.CurrentUserService;
import org.example.bookshop.service.cart.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CartController implements CartApi {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    @Override
    public ResponseEntity<CartDto> getCart(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(cartService.getCurrentCart(user));
    }

    @Override
    public ResponseEntity<CartItemDto> addItem(Authentication authentication, AddToCartRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        CartItemDto dto = cartService.addItem(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<CartItemDto> updateQuantity(Authentication authentication, Long bookId,
                                                     UpdateCartItemRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(cartService.updateQuantity(user, bookId, request.getQuantity()));
    }

    @Override
    public ResponseEntity<Void> removeItem(Authentication authentication, Long bookId) {
        User user = currentUserService.getCurrentUser(authentication);
        cartService.removeItem(user, bookId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}