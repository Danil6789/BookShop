package org.example.bookshop.controller;

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
class CategoryControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        adminToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "admin", "password");
        userToken = "Bearer " + AuthTestHelper.loginAs(mockMvc, objectMapper, "ivan", "password");
    }

    @Test
    void findAll_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
            .andExpect(jsonPath("$[0].name", equalTo("Детектив")));
    }

    @Test
    void findAll_noAuth_returns5Categories() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(5)));
    }

    @Test
    void create_asAdmin_returns201() throws Exception {
        String body = "{\"name\":\"Поэзия\"}";
        mockMvc.perform(post("/api/categories")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name", equalTo("Поэзия")));
    }

    @Test
    void create_asUser_returns403() throws Exception {
        String body = "{\"name\":\"Запрет\"}";
        mockMvc.perform(post("/api/categories")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_noAuth_returns401() throws Exception {
        String body = "{\"name\":\"БезАвторизации\"}";
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        String body = "{\"name\":\"Детектив\"}";
        mockMvc.perform(post("/api/categories")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", equalTo(409)));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        String body = "{\"name\":\"\"}";
        mockMvc.perform(post("/api/categories")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void update_asAdmin_returns200() throws Exception {
        String body = "{\"name\":\"Поэзия обновлённая\"}";
        mockMvc.perform(put("/api/categories/1")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(1)))
            .andExpect(jsonPath("$.name", equalTo("Поэзия обновлённая")));
    }

    @Test
    void update_missingCategory_returns404() throws Exception {
        String body = "{\"name\":\"Что-то\"}";
        mockMvc.perform(put("/api/categories/999")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void update_asUser_returns403() throws Exception {
        String body = "{\"name\":\"Нельзя\"}";
        mockMvc.perform(put("/api/categories/1")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void delete_categoryWithBooks_returns409() throws Exception {
        // category id=1 (Художественная литература) имеет книги
        mockMvc.perform(delete("/api/categories/1")
                .header("Authorization", adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", equalTo(409)));
    }

    @Test
    void delete_missingCategory_returns404() throws Exception {
        mockMvc.perform(delete("/api/categories/999")
                .header("Authorization", adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                .header("Authorization", userToken))
            .andExpect(status().isForbidden());
    }
}
