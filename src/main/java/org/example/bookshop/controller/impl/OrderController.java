package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.controller.OrderApi;
import org.example.bookshop.dto.order.OrderDto;
import org.example.bookshop.entity.User;
import org.example.bookshop.service.auth.CurrentUserService;
import org.example.bookshop.service.order.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @Override
    public ResponseEntity<OrderDto> checkout(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        OrderDto order = orderService.checkout(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
