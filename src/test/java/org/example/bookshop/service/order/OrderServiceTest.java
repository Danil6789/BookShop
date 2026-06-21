package org.example.bookshop.service.order;

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

    @Test
    void checkout_insufficientStock_throwsInsufficientStock() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        book.setStock(2);
        CartItem item = CartItem.builder().id(10L).user(ivan).book(book).quantity(5).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.checkout(ivan))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("id=1")
            .hasMessageContaining("запрошено 5")
            .hasMessageContaining("доступно 2");

        verify(orderRepository, never()).save(any(Order.class));
        verify(bookRepository, never()).save(any(Book.class));
        verify(cartItemRepository, never()).deleteByUserId(any());
    }

    @Test
    void checkout_validCart_decrementsBookStock() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        book.setStock(10);
        CartItem item = CartItem.builder().id(10L).user(ivan).book(book).quantity(3).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        service.checkout(ivan);

        assertThat(book.getStock()).isEqualTo(7);
        verify(bookRepository).save(book);
    }

    @Test
    void checkout_validCart_clearsCart() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        CartItem item = CartItem.builder().id(10L).user(ivan).book(book).quantity(1).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        service.checkout(ivan);

        verify(cartItemRepository).deleteByUserId(2L);
    }

    @Test
    void checkout_multipleItems_validatesAllStockFirst() {
        Book book1 = book(1L, "Мастер и Маргарита", "650.00");
        book1.setStock(10);
        Book book2 = book(2L, "1984", "550.00");
        book2.setStock(2);
        CartItem item1 = CartItem.builder().id(10L).user(ivan).book(book1).quantity(2).build();
        CartItem item2 = CartItem.builder().id(11L).user(ivan).book(book2).quantity(5).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item1, item2));

        assertThatThrownBy(() -> service.checkout(ivan))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("id=2");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_stockEqualsQuantity_succeeds() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        book.setStock(3);
        CartItem item = CartItem.builder().id(10L).user(ivan).book(book).quantity(3).build();
        when(cartItemRepository.findByUserId(2L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setStatus(OrderStatus.PENDING);
            return arg;
        });

        OrderDto result = service.checkout(ivan);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(book.getStock()).isEqualTo(0);
    }

    @Test
    void getUserOrders_returnsUsersOrders() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order1 = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        order1.getItems().add(OrderItem.builder().order(order1).book(book).quantity(1)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        Order order2 = Order.builder().id(2L).user(ivan).totalAmount(new BigDecimal("1300.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        when(orderRepository.findByUserIdOrderByIdDesc(2L)).thenReturn(List.of(order1, order2));

        List<OrderDto> result = service.getUserOrders(ivan);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(orderRepository).findByUserIdOrderByIdDesc(2L);
    }

    @Test
    void getAllOrders_returnsAllOrders() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order1 = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        Order order2 = Order.builder().id(2L).user(ivan).totalAmount(new BigDecimal("1300.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        Order order3 = Order.builder().id(3L).user(ivan).totalAmount(new BigDecimal("850.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(List.of(order1, order2, order3));

        List<OrderDto> result = service.getAllOrders();

        assertThat(result).hasSize(3);
        verify(orderRepository).findAllByOrderByIdDesc();
    }

    @Test
    void getOrderById_ownOrder_returnsDto() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        order.getItems().add(OrderItem.builder().order(order).book(book).quantity(1)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto result = service.getOrderById(ivan, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void getOrderById_otherUserOrder_throwsAccessDenied() {
        User petr = User.builder().id(3L).username("petr").role(User.Role.USER).build();
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        order.getItems().add(OrderItem.builder().order(order).book(book).quantity(1)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getOrderById(petr, 1L))
            .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void getOrderById_missingOrder_throwsNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrderById(ivan, 999L))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void updateStatus_existingOrder_updatesAndSaves() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        order.getItems().add(OrderItem.builder().order(order).book(book).quantity(1)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = service.updateStatus(1L, OrderStatus.PAID);

        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_missingOrder_throwsNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(999L, OrderStatus.PAID))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessageContaining("999");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancel_ownPendingOrder_restoresStockAndUpdatesStatus() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        book.setStock(7);
        Order order = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("1300.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        order.getItems().add(OrderItem.builder().order(order).book(book).quantity(3)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = service.cancelOrder(ivan, 1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(book.getStock()).isEqualTo(10);
        verify(bookRepository).save(book);
        verify(orderRepository).save(order);
    }

    @Test
    void cancel_otherUserOrder_throwsAccessDenied() {
        User petr = User.builder().id(3L).username("petr").role(User.Role.USER).build();
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING).items(new java.util.ArrayList<>()).build();
        order.getItems().add(OrderItem.builder().order(order).book(book).quantity(1)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(petr, 1L))
            .isInstanceOf(OrderAccessDeniedException.class);

        verify(bookRepository, never()).save(any(Book.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancel_nonPendingOrder_throwsNotCancellable() {
        Book book = book(1L, "Мастер и Маргарита", "650.00");
        Order order = Order.builder().id(1L).user(ivan).totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PAID).items(new java.util.ArrayList<>()).build();
        order.getItems().add(OrderItem.builder().order(order).book(book).quantity(1)
            .priceAtPurchase(new BigDecimal("650.00")).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(ivan, 1L))
            .isInstanceOf(OrderNotCancellableException.class)
            .hasMessageContaining("id=1")
            .hasMessageContaining("PAID");

        verify(bookRepository, never()).save(any(Book.class));
        verify(orderRepository, never()).save(any(Order.class));
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
