package org.example.bookshop.service.cart;

import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.dto.cart.CartDto;
import org.example.bookshop.dto.cart.CartItemDto;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.CartItem;
import org.example.bookshop.entity.User;
import org.example.bookshop.exception.cart.CartItemNotFoundException;
import org.example.bookshop.exception.catalog.BookNotFoundException;
import org.example.bookshop.mapper.CartItemMapper;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private BookRepository bookRepository;
    private final CartItemMapper cartItemMapper = Mappers.getMapper(CartItemMapper.class);
    private CartService service;

    private final User ivan = User.builder().id(2L).username("ivan").build();

    @BeforeEach
    void setUp() {
        service = new CartService(cartItemRepository, bookRepository, cartItemMapper);
    }

    @Test
    void getCurrentCart_empty_returnsEmptyDto() {
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of());

        CartDto result = service.getCurrentCart(ivan);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalQuantity()).isZero();
        assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCurrentCart_withItems_aggregatesTotals() {
        Book book1 = book(1L, "Мастер и Маргарита", "100.00");
        Book book2 = book(2L, "Преступление и наказание", "50.00");
        CartItem item1 = CartItem.builder().id(10L).user(ivan).book(book1).quantity(2).build();
        CartItem item2 = CartItem.builder().id(11L).user(ivan).book(book2).quantity(3).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item1, item2));

        CartDto result = service.getCurrentCart(ivan);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalQuantity()).isEqualTo(5);
        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(result.getItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.getItems().get(1).getSubtotal()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void addItem_newBook_createsCartItem() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        AddToCartRequest req = new AddToCartRequest();
        req.setBookId(1L);
        req.setQuantity(2);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserIdAndBookId(2L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem arg = inv.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(100L);
            }
            return arg;
        });

        CartItemDto result = service.addItem(ivan, req);

        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Мастер и Маргарита");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("650.00"));
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("1300.00"));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_existingBook_incrementsQuantity() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        CartItem existing = CartItem.builder().id(50L).user(ivan).book(book).quantity(3).build();
        AddToCartRequest req = new AddToCartRequest();
        req.setBookId(1L);
        req.setQuantity(2);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserIdAndBookId(2L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemDto result = service.addItem(ivan, req);

        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("3250.00"));
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addItem_missingBook_throwsBookNotFoundException() {
        AddToCartRequest req = new AddToCartRequest();
        req.setBookId(999L);
        req.setQuantity(1);

        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(ivan, req))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void updateQuantity_existing_setsNewQuantity() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        CartItem existing = CartItem.builder().id(50L).user(ivan).book(book).quantity(3).build();
        when(cartItemRepository.findByUserIdAndBookId(2L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemDto result = service.updateQuantity(ivan, 1L, 10);

        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("6500.00"));
        verify(cartItemRepository).save(existing);
    }

    @Test
    void updateQuantity_missing_throwsCartItemNotFoundException() {
        when(cartItemRepository.findByUserIdAndBookId(2L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateQuantity(ivan, 999L, 5))
            .isInstanceOf(CartItemNotFoundException.class)
            .hasMessageContaining("999");
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void removeItem_existing_deletesIt() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        CartItem existing = CartItem.builder().id(50L).user(ivan).book(book).quantity(3).build();
        when(cartItemRepository.findByUserIdAndBookId(2L, 1L)).thenReturn(Optional.of(existing));

        service.removeItem(ivan, 1L);

        verify(cartItemRepository).delete(existing);
    }

    @Test
    void removeItem_missing_throwsCartItemNotFoundException() {
        when(cartItemRepository.findByUserIdAndBookId(2L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeItem(ivan, 999L))
            .isInstanceOf(CartItemNotFoundException.class)
            .hasMessageContaining("999");
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void clearCart_deletesAllForUser() {
        service.clearCart(ivan);

        verify(cartItemRepository).deleteByUserId(2L);
    }

    private static Book book(Long id, String title, String price) {
        return Book.builder()
            .id(id)
            .title(title)
            .price(new BigDecimal(price))
            .stock(10)
            .build();
    }
}