package org.example.bookshop.controller;

import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.repository.BookRepository;
import org.example.bookshop.repository.CartItemRepository;
import org.example.bookshop.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

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
class OrderControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;

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

    @Test
    void getMyOrders_asUser_returns200WithOwnOrders() throws Exception {
        addToCartAndCheckout(ivanToken, 1L, 2);

        mockMvc.perform(get("/api/orders")
                .header("Authorization", ivanToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].totalAmount").value(1300.00));
    }

    @Test
    void getAllOrders_asAdmin_returnsAll() throws Exception {
        addToCartAndCheckout(ivanToken, 1L, 1);
        String adminToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "admin", "password");

        mockMvc.perform(get("/api/orders")
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getById_asUser_ownOrder_returns200() throws Exception {
        Long orderId = addToCartAndCheckout(ivanToken, 1L, 1);

        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", ivanToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getById_asUser_otherUserOrder_returns403() throws Exception {
        Long orderId = addToCartAndCheckout(ivanToken, 1L, 1);

        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", ivanToken))
            .andExpect(status().isOk());

        registerUser("petr", "password");
        String petrToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "petr", "password");
        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", petrToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getById_missingOrder_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/9999")
                .header("Authorization", ivanToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("9999")));
    }

    @Test
    void updateStatus_asAdmin_returns200() throws Exception {
        Long orderId = addToCartAndCheckout(ivanToken, 1L, 1);
        String adminToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "admin", "password");

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "PAID"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void updateStatus_asUser_returns403() throws Exception {
        Long orderId = addToCartAndCheckout(ivanToken, 1L, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", ivanToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "PAID"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void cancel_asUser_ownPending_returns200_restoresStock() throws Exception {
        int initialStock = bookRepository.findById(1L).orElseThrow().getStock();
        Long orderId = addToCartAndCheckout(ivanToken, 1L, 2);

        int stockAfterCheckout = bookRepository.findById(1L).orElseThrow().getStock();
        assertThat(stockAfterCheckout).isEqualTo(initialStock - 2);

        mockMvc.perform(delete("/api/orders/" + orderId)
                .header("Authorization", ivanToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        int stockAfterCancel = bookRepository.findById(1L).orElseThrow().getStock();
        assertThat(stockAfterCancel).isEqualTo(initialStock);
    }

    @Test
    void cancel_alreadyShipped_returns409() throws Exception {
        Long orderId = addToCartAndCheckout(ivanToken, 1L, 1);
        String adminToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "admin", "password");

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SHIPPED"))))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/orders/" + orderId)
                .header("Authorization", ivanToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("SHIPPED")));
    }

    private static AddToCartRequest addReq(Long bookId, Integer quantity) {
        AddToCartRequest r = new AddToCartRequest();
        r.setBookId(bookId);
        r.setQuantity(quantity);
        return r;
    }

    private Long addToCartAndCheckout(String token, Long bookId, int qty) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(bookId, qty))))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", token))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode tree = objectMapper.readTree(result.getResponse().getContentAsString());
        return tree.get("id").asLong();
    }

    private void registerUser(String username, String password) throws Exception {
        Map<String, String> body = Map.of(
            "username", username,
            "password", password,
            "role", "USER"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
    }
}
