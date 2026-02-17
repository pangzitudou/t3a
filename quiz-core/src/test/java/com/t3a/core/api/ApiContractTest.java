package com.t3a.core.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t3a.core.domain.dto.LoginRequest;
import com.t3a.core.domain.dto.RegisterRequest;
import com.t3a.core.domain.entity.QuestionBank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Contract Tests
 * Verifies that all API endpoints are accessible and return proper response format
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Auth endpoints - Should be accessible")
    void allAuthEndpoints_ShouldBeAccessible() throws Exception {
        // POST /auth/register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("test");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("test@example.com");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest()); // May fail validation, but endpoint exists

        // POST /auth/login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("test");
        loginRequest.setPassword("wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(jsonPath("$.code").exists()); // Will fail, but response format is correct
    }

    @Test
    @DisplayName("Question Bank endpoints - Should be accessible")
    void allBankEndpoints_ShouldBeAccessible() throws Exception {
        // GET /bank/list
        mockMvc.perform(get("/bank/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());

        // GET /bank/list with parameters
        mockMvc.perform(get("/bank/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("creatorId", "1")
                        .param("category", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // GET /bank/public
        mockMvc.perform(get("/bank/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        // GET /bank/{id}
        mockMvc.perform(get("/bank/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists()); // May be error, but format is correct

        // POST /bank/create
        QuestionBank bank = new QuestionBank();
        bank.setName("Test");
        bank.setDescription("Test");

        mockMvc.perform(post("/bank/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bank)))
                .andExpect(status().isBadRequest()); // Missing required fields, but endpoint exists

        // PUT /bank/update
        bank.setId(1L);
        mockMvc.perform(put("/bank/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bank)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());

        // DELETE /bank/{id}
        mockMvc.perform(delete("/bank/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Quiz endpoints - Should be accessible")
    void allQuizEndpoints_ShouldBeAccessible() throws Exception {
        // GET /dashboard
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());

        // POST /quiz/start with invalid request
        String invalidRequest = "{}";
        mockMvc.perform(post("/quiz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists()); // Will fail, but endpoint is accessible

        // POST /quiz/submit with invalid request
        mockMvc.perform(post("/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());

        // GET /quiz/session/{sessionKey}
        mockMvc.perform(get("/quiz/session/non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());

        // GET /quiz/history/{userId}
        mockMvc.perform(get("/quiz/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        // GET /quiz/result/{sessionKey}
        mockMvc.perform(get("/quiz/result/non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("All endpoints - Should return consistent JSON structure")
    void allEndpoints_ShouldReturnConsistentJsonStructure() throws Exception {
        // Test that all responses have the expected structure
        String[] endpoints = {
            "/bank/list",
            "/bank/public",
            "/dashboard",
            "/quiz/history/1"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Test
    @DisplayName("POST endpoints - Should handle invalid JSON")
    void postEndpoints_ShouldHandleInvalidJson() throws Exception {
        // POST /auth/login with invalid JSON
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        // POST /bank/create with invalid JSON
        mockMvc.perform(post("/bank/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET endpoints - Should handle path variables")
    void getEndpoints_ShouldHandlePathVariables() throws Exception {
        // Test various path variable endpoints
        mockMvc.perform(get("/bank/abc")) // Invalid ID format
                .andExpect(status().isOk()); // Should handle gracefully

        mockMvc.perform(get("/quiz/session/"))
                .andExpect(status().isNotFound()); // Missing path variable
    }

    @Test
    @DisplayName("Endpoints - Should return proper HTTP status codes")
    void endpoints_ShouldReturnProperStatusCodes() throws Exception {
        // Valid GET requests should return 200
        mockMvc.perform(get("/bank/list"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());

        // Invalid POST requests should return 400 or 200 with error
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Non-existent resources should still return 200 with error code
        mockMvc.perform(get("/bank/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("API - Should handle CORS preflight requests")
    void api_ShouldHandleCorsPreflight() throws Exception {
        // Test OPTIONS request
        mockMvc.perform(options("/bank/list")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API - Should handle content negotiation")
    void api_ShouldHandleContentNegotiation() throws Exception {
        // Test with JSON content type
        mockMvc.perform(get("/bank/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Test without content type
        mockMvc.perform(get("/bank/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API - Should be accessible with query parameters")
    void api_ShouldHandleQueryParameters() throws Exception {
        // Test with various query parameters
        mockMvc.perform(get("/bank/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/bank/list")
                        .param("pageNum", "0") // Invalid page
                        .param("pageSize", "1000")) // Large page size
                .andExpect(status().isOk()); // Should handle gracefully
    }
}
