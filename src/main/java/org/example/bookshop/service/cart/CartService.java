package org.example.bookshop.service.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.dto.cart.CartDto;
import org.example.bookshop.dto.cart.CartItemDto;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.CartItem;
import org.example.bookshop.entity.User;
import org.example.bookshop.exception.catalog.BookNotFoundException;
import org.example.bookshop.mapper.CartItemMapper;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final CartItemMapper cartItemMapper;

    public CartDto getCurrentCart(User user) {
        log.debug("Loading cart for user={}", user.getId());

        List<CartItem> items = cartItemRepository.findByUserId(user.getId());
        List<CartItemDto> dtos = items.stream()
            .map(cartItemMapper::toDto)
            .toList();

        int totalQuantity = dtos.stream()
            .mapToInt(CartItemDto::getQuantity)
            .sum();

        BigDecimal totalPrice = dtos.stream()
            .map(CartItemDto::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(dtos, totalQuantity, totalPrice);
    }

    @Transactional
    public CartItemDto addItem(User user, AddToCartRequest request) {
        log.debug("Adding book={} (qty={}) to cart for user={}",
            request.getBookId(), request.getQuantity(), user.getId());

        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new BookNotFoundException(request.getBookId()));

        CartItem item = cartItemRepository.findByUserIdAndBookId(user.getId(), book.getId())
            .map(existing -> {
                existing.setQuantity(existing.getQuantity() + request.getQuantity());
                return existing;
            })
            .orElseGet(() -> CartItem.builder()
                .user(user)
                .book(book)
                .quantity(request.getQuantity())
                .build());

        CartItem saved = cartItemRepository.save(item);
        return cartItemMapper.toDto(saved);
    }
}