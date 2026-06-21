package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.controller.OrderApi;
import org.example.bookshop.dto.order.OrderDto;
import org.example.bookshop.dto.order.UpdateStatusRequest;
import org.example.bookshop.entity.User;
import org.example.bookshop.service.auth.CurrentUserService;
import org.example.bookshop.service.order.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Override
    public ResponseEntity<List<OrderDto>> getOrders(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        List<OrderDto> orders = user.getRole() == User.Role.ADMIN
            ? orderService.getAllOrders()
            : orderService.getUserOrders(user);
        return ResponseEntity.ok(orders);
    }

    @Override
    public ResponseEntity<OrderDto> getById(Authentication authentication, Long id) {
        User user = currentUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(orderService.getOrderById(user, id));
    }

    @Override
    public ResponseEntity<OrderDto> updateStatus(Long id, UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }

    @Override
    public ResponseEntity<OrderDto> cancel(Authentication authentication, Long id) {
        User user = currentUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(orderService.cancelOrder(user, id));
    }
}
