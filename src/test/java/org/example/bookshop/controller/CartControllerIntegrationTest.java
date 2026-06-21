package org.example.bookshop.controller;

import tools.jackson.databind.ObjectMapper;
import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.dto.cart.UpdateCartItemRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CartItemRepository cartItemRepository;

    private String ivanToken;
    private static final Long IVAN_ID = 2L;

    @BeforeEach
    void obtainToken() throws Exception {
        ivanToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "ivan", "password");
    }

    @Test
    void getCart_empty_returns200WithEmptyCart() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", ivanToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.totalQuantity").value(0))
            .andExpect(jsonPath("$.totalPrice").value(0));
    }

    @Test
    void getCart_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/cart"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_newBook_returns201() throws Exception {
        AddToCartRequest req = addReq(1L, 2);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bookId").value(1))
            .andExpect(jsonPath("$.title").value("Мастер и Маргарита"))
            .andExpect(jsonPath("$.price").value(650.00))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.subtotal").value(1300.00));

        assertThat(cartItemRepository.findByUserIdAndBookId(IVAN_ID, 1L))
            .isPresent()
            .get()
            .satisfies(item -> {
                assertThat(item.getQuantity()).isEqualTo(2);
                assertThat(item.getUser().getId()).isEqualTo(IVAN_ID);
            });
    }

    @Test
    void addItem_existingBook_incrementsQuantity() throws Exception {
        AddToCartRequest req = addReq(1L, 2);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quantity").value(4));
    }

    @Test
    void updateQuantity_existingItem_setsNewValue() throws Exception {
        AddToCartRequest add = addReq(1L, 2);
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(add)))
            .andExpect(status().isCreated());

        UpdateCartItemRequest update = updateReq(10);
        mockMvc.perform(patch("/api/cart/items/{bookId}", 1L)
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(10))
            .andExpect(jsonPath("$.subtotal").value(6500.00));
    }

    @Test
    void removeItem_existing_returns204() throws Exception {
        AddToCartRequest add = addReq(1L, 2);
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(add)))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/cart/items/{bookId}", 1L)
                .header("Authorization", ivanToken))
            .andExpect(status().isNoContent());

        assertThat(cartItemRepository.findByUserIdAndBookId(IVAN_ID, 1L)).isEmpty();
    }

    @Test
    void clearCart_returns204() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(1L, 1))))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(2L, 1))))
            .andExpect(status().isCreated());

        assertThat(cartItemRepository.findByUserId(IVAN_ID)).hasSize(2);

        mockMvc.perform(delete("/api/cart")
                .header("Authorization", ivanToken))
            .andExpect(status().isNoContent());

        assertThat(cartItemRepository.findByUserId(IVAN_ID)).isEmpty();
    }

    private static AddToCartRequest addReq(Long bookId, Integer quantity) {
        AddToCartRequest r = new AddToCartRequest();
        r.setBookId(bookId);
        r.setQuantity(quantity);
        return r;
    }

    private static UpdateCartItemRequest updateReq(Integer quantity) {
        UpdateCartItemRequest r = new UpdateCartItemRequest();
        r.setQuantity(quantity);
        return r;
    }
}