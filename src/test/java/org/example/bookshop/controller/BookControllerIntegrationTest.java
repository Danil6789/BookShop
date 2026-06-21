package org.example.bookshop.controller;

import org.example.bookshop.entity.Book;
import org.example.bookshop.entity.Order;
import org.example.bookshop.entity.OrderItem;
import org.example.bookshop.entity.OrderStatus;
import org.example.bookshop.entity.User;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.OrderItemRepository;
import org.example.bookshop.repository.OrderRepository;
import org.example.bookshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private UserRepository userRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        adminToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "admin", "password");
        userToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "ivan", "password");
    }

    @Test
    void findAll_returnsAllSeededBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(8)))
            .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(8)))
            .andExpect(jsonPath("$.content[0].id", equalTo(1)))
            .andExpect(jsonPath("$.content[0].categoryName", equalTo("Художественная литература")));
    }

    @Test
    void findAllWithSize_returnsPaged() throws Exception {
        mockMvc.perform(get("/api/books?size=3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(8)))
            .andExpect(jsonPath("$.totalPages", equalTo(3)))
            .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    void findById_existing_returnsBook() throws Exception {
        mockMvc.perform(get("/api/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(1)))
            .andExpect(jsonPath("$.title", equalTo("Мастер и Маргарита")))
            .andExpect(jsonPath("$.categoryId", equalTo(1)))
            .andExpect(jsonPath("$.categoryName", equalTo("Художественная литература")));
    }

    @Test
    void findById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/books/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void search_byCategoryId_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/books/search?categoryId=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(2)))
            .andExpect(jsonPath("$.content[0].id", equalTo(2)))
            .andExpect(jsonPath("$.content[1].id", equalTo(7)));
    }

    @Test
    void search_byPriceRange_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/books/search?minPrice=500&maxPrice=700"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(2)));
    }

    @Test
    void search_conflictingPriceRange_returnsEmptyNotError() throws Exception {
        mockMvc.perform(get("/api/books/search?minPrice=1000&maxPrice=500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(0)));
    }

    @Test
    void search_byTitleSubstring_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/books/search").param("q", "Мастер"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(1)))
            .andExpect(jsonPath("$.content[0].title", equalTo("Мастер и Маргарита")));
    }

    @Test
    void categoriesList_returnsAllSeeded() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
            .andExpect(jsonPath("$[0].name", equalTo("Детектив")));
    }

    @Test
    void create_asAdmin_returns201_withCategory() throws Exception {
        String body = """
            {"title":"Новая книга","description":"Описание","price":100.00,"stock":10,
             "coverUrl":"new.jpg","categoryId":1}
            """;
        mockMvc.perform(post("/api/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title", equalTo("Новая книга")))
            .andExpect(jsonPath("$.price", equalTo(100.00)))
            .andExpect(jsonPath("$.categoryId", equalTo(1)));
    }

    @Test
    void create_asAdmin_returns201_withoutCategory() throws Exception {
        String body = """
            {"title":"Без категории","price":50.00,"stock":5}
            """;
        mockMvc.perform(post("/api/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoryId").doesNotExist());
    }

    @Test
    void create_asUser_returns403() throws Exception {
        String body = "{\"title\":\"X\",\"price\":10.00,\"stock\":1}";
        mockMvc.perform(post("/api/books")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        String body = "{\"title\":\"\",\"price\":-1.00,\"stock\":-1}";
        mockMvc.perform(post("/api/books")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void update_asAdmin_returns200() throws Exception {
        String body = """
            {"title":"Мастер и Маргарита (обновл.)","price":700.00,"stock":20,"categoryId":1}
            """;
        mockMvc.perform(put("/api/books/1")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(1)))
            .andExpect(jsonPath("$.title", equalTo("Мастер и Маргарита (обновл.)")))
            .andExpect(jsonPath("$.price", equalTo(700.00)));
    }

    @Test
    void delete_bookNotInOrders_returns204() throws Exception {
        long beforeCount = bookRepository.count();
        mockMvc.perform(delete("/api/books/8")
                .header("Authorization", adminToken))
            .andExpect(status().isNoContent());
        assertThat(bookRepository.count()).isEqualTo(beforeCount - 1);
    }

    @Test
    void delete_bookInOrders_returns409() throws Exception {
        // book id=1 (Мастер и Маргарита) — сначала создадим order с ним
        User ivan = userRepository.findByUsername("ivan").orElseThrow();
        Book book1 = bookRepository.findById(1L).orElseThrow();
        Order order = Order.builder()
            .user(ivan)
            .totalAmount(new BigDecimal("650.00"))
            .status(OrderStatus.PENDING)
            .build();
        Order savedOrder = orderRepository.save(order);
        OrderItem item = OrderItem.builder()
            .order(savedOrder)
            .book(book1)
            .quantity(1)
            .priceAtPurchase(new BigDecimal("650.00"))
            .build();
        orderItemRepository.save(item);

        mockMvc.perform(delete("/api/books/1")
                .header("Authorization", adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", equalTo(409)));
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/books/1")
                .header("Authorization", userToken))
            .andExpect(status().isForbidden());
    }
}
