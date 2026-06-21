package org.example.bookshop.controller;

import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CartItemRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private CartItemRepository cartItemRepository;

    private String ivanToken;
    private static final Long IVAN_ID = 2L;

    @BeforeEach
    void obtainToken() throws Exception {
        ivanToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "ivan", "password");
    }

    @Test
    void checkout_emptyCart_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Корзина пуста")));
    }

    @Test
    void checkout_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_validCart_returns201() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(1L, 2))))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(2L, 1))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalAmount").value(1850.00))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].bookId").value(1))
            .andExpect(jsonPath("$.items[0].title").value("Мастер и Маргарита"))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[1].bookId").value(2));
    }

    @Test
    void checkout_validCart_clearsCartAfterCheckout() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(1L, 1))))
            .andExpect(status().isCreated());

        assertThat(cartItemRepository.findByUserId(IVAN_ID)).hasSize(1);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isCreated());

        assertThat(cartItemRepository.findByUserId(IVAN_ID)).isEmpty();
    }

    @Test
    void checkout_validCart_decrementsBookStock() throws Exception {
        int initialStock = bookRepository.findById(1L).orElseThrow().getStock();

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(1L, 2))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isCreated());

        int finalStock = bookRepository.findById(1L).orElseThrow().getStock();
        assertThat(finalStock).isEqualTo(initialStock - 2);
    }

    @Test
    void checkout_insufficientStock_returns409() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(1L, 20))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("id=1")));
    }

    @Test
    void checkout_capturesPriceAtPurchase() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(1L, 1))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].priceAtPurchase").value(650.00))
            .andExpect(jsonPath("$.items[0].subtotal").value(650.00));
    }

    private static AddToCartRequest addReq(Long bookId, Integer quantity) {
        AddToCartRequest r = new AddToCartRequest();
        r.setBookId(bookId);
        r.setQuantity(quantity);
        return r;
    }
}
