package org.example.bookshop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
            .andExpect(jsonPath("$.content[0].id", equalTo(2))) // 1984
            .andExpect(jsonPath("$.content[1].id", equalTo(7))); // Дюна
    }

    @Test
    void search_byPriceRange_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/books/search?minPrice=500&maxPrice=700"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", equalTo(2)));
    }

    @Test
    void search_conflictingPriceRange_returnsEmptyNotError() throws Exception {
        // minPrice > maxPrice -> пустой список, не 400
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
            .andExpect(jsonPath("$[0].name", equalTo("Детектив"))); // сортировка по имени
    }
}
