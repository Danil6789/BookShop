package org.example.bookshop.service.order;

import org.example.bookshop.dto.order.OrderDto;
import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.CartItem;
import org.example.bookshop.entity.Order;
import org.example.bookshop.entity.OrderStatus;
import org.example.bookshop.entity.User;
import org.example.bookshop.exception.order.EmptyCartException;
import org.example.bookshop.mapper.OrderItemMapper;
import org.example.bookshop.mapper.OrderMapper;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CartItemRepository;
import org.example.bookshop.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private BookRepository bookRepository;
    @Mock private OrderRepository orderRepository;
    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);
    private final OrderItemMapper orderItemMapper = Mappers.getMapper(OrderItemMapper.class);
    private OrderService service;

    private final User ivan = User.builder().id(2L).username("ivan").build();

    @BeforeEach
    void setUp() {
        service = new OrderService(cartItemRepository, bookRepository, orderRepository,
            orderMapper, orderItemMapper);
    }

    @Test
    void checkout_emptyCart_throwsEmptyCartException() {
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.checkout(ivan))
            .isInstanceOf(EmptyCartException.class)
            .hasMessageContaining("Корзина пуста");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_validCart_buildsOrderWithItems() {
        Book book1 = book(1L, "Мастер и Маргарита", "650.00");
        Book book2 = book(2L, "1984", "550.00");
        CartItem item1 = CartItem.builder().id(10L).user(ivan).book(book1).quantity(2).build();
        CartItem item2 = CartItem.builder().id(11L).user(ivan).book(book2).quantity(1).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item1, item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        OrderDto result = service.checkout(ivan);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getBookId()).isEqualTo(1L);
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("Мастер и Маргарита");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(1).getBookId()).isEqualTo(2L);
        assertThat(result.getItems().get(1).getQuantity()).isEqualTo(1);
    }

    @Test
    void checkout_validCart_savesOrder() {
        Book book1 = book(1L, "Мастер и Маргарита", "650.00");
        CartItem item1 = CartItem.builder().id(10L).user(ivan).book(book1).quantity(2).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item1));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        service.checkout(ivan);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void checkout_calculatesTotalAmount_correctly() {
        Book book1 = book(1L, "Мастер и Маргарита", "650.00");
        Book book2 = book(2L, "1984", "550.00");
        Book book3 = book(3L, "Преступление и наказание", "450.00");
        CartItem item1 = CartItem.builder().id(10L).user(ivan).book(book1).quantity(2).build();
        CartItem item2 = CartItem.builder().id(11L).user(ivan).book(book2).quantity(3).build();
        CartItem item3 = CartItem.builder().id(12L).user(ivan).book(book3).quantity(1).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item1, item2, item3));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        OrderDto result = service.checkout(ivan);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3400.00"));
    }

    @Test
    void checkout_capturesPriceAtPurchase_fromBookPrice() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        CartItem item = CartItem.builder().id(10L).user(ivan).book(book).quantity(3).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        OrderDto result = service.checkout(ivan);

        assertThat(result.getItems().get(0).getPriceAtPurchase())
            .isEqualByComparingTo(new BigDecimal("650.00"));
        assertThat(result.getItems().get(0).getSubtotal())
            .isEqualByComparingTo(new BigDecimal("1950.00"));
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
