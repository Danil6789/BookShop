package org.example.bookshop.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookshop.dto.order.OrderDto;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.CartItem;
import org.example.bookshop.entity.Order;
import org.example.bookshop.entity.OrderItem;
import org.example.bookshop.entity.OrderStatus;
import org.example.bookshop.entity.User;
import org.example.bookshop.exception.order.EmptyCartException;
import org.example.bookshop.exception.order.InsufficientStockException;
import org.example.bookshop.exception.order.OrderAccessDeniedException;
import org.example.bookshop.exception.order.OrderNotCancellableException;
import org.example.bookshop.exception.order.OrderNotFoundException;
import org.example.bookshop.mapper.OrderItemMapper;
import org.example.bookshop.mapper.OrderMapper;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CartItemRepository;
import org.example.bookshop.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Transactional
    public OrderDto checkout(User user) {
        log.debug("Checkout cart for user={}", user.getId());

        List<CartItem> items = cartItemRepository.findByUserId(user.getId());
        if (items.isEmpty()) {
            throw new EmptyCartException();
        }

        for (CartItem item : items) {
            Book book = item.getBook();
            if (book.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(book.getId(), item.getQuantity(), book.getStock());
            }
        }

        Order order = Order.builder()
            .user(user)
            .items(new ArrayList<>())
            .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            Book book = item.getBook();
            BigDecimal priceAtPurchase = book.getPrice();
            OrderItem oi = OrderItem.builder()
                .order(order)
                .book(book)
                .quantity(item.getQuantity())
                .priceAtPurchase(priceAtPurchase)
                .build();
            order.getItems().add(oi);
            total = total.add(priceAtPurchase.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        for (CartItem item : items) {
            Book book = item.getBook();
            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);
        }

        cartItemRepository.deleteByUserId(user.getId());

        return toDto(saved);
    }

    public List<OrderDto> getUserOrders(User user) {
        log.debug("List orders for user={}", user.getId());
        return orderRepository.findByUserIdOrderByIdDesc(user.getId()).stream()
            .map(this::toDto)
            .toList();
    }

    public List<OrderDto> getAllOrders() {
        log.debug("List all orders (admin)");
        return orderRepository.findAllByOrderByIdDesc().stream()
            .map(this::toDto)
            .toList();
    }

    public OrderDto getOrderById(User user, Long orderId) {
        log.debug("Get order id={} for user={}", orderId, user.getId());
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new OrderAccessDeniedException();
        }
        return toDto(order);
    }

    @Transactional
    public OrderDto updateStatus(Long orderId, OrderStatus newStatus) {
        log.debug("Update order id={} status to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    @Transactional
    public OrderDto cancelOrder(User user, Long orderId) {
        log.debug("Cancel order id={} for user={}", orderId, user.getId());
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new OrderAccessDeniedException();
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotCancellableException(orderId, order.getStatus().name());
        }
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            book.setStock(book.getStock() + item.getQuantity());
            bookRepository.save(book);
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    private OrderDto toDto(Order order) {
        OrderDto dto = orderMapper.toDto(order);
        dto.setItems(order.getItems().stream().map(orderItemMapper::toDto).toList());
        return dto;
    }
}
