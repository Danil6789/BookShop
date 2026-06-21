package org.example.bookshop.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public final class AuthTestHelper {

    private AuthTestHelper() {
    }

    public static String loginAs(MockMvc mockMvc, ObjectMapper objectMapper, String username, String password)
        throws Exception {
        Map<String, String> body = Map.of("username", username, "password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        String response = result.getResponse().getContentAsString();
        JsonNode tree = objectMapper.readTree(response);
        return tree.get("token").asText();
    }
}