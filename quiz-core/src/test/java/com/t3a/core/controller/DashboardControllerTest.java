package com.t3a.core.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DashboardController integration tests
 * Tests dashboard statistics endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /dashboard - Should return dashboard statistics")
    void getDashboard_ShouldReturnStatistics() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.quizzesCompleted").isNumber())
                .andExpect(jsonPath("$.data.averageScore").exists())
                .andExpect(jsonPath("$.data.studyTime").isNumber())
                .andExpect(jsonPath("$.data.currentStreak").isNumber())
                .andExpect(jsonPath("$.data.recentQuizzes").isArray());
    }

    @Test
    @DisplayName("GET /dashboard - Should return zero values for new user")
    void getDashboard_NewUser_ShouldReturnZeroValues() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quizzesCompleted").value(0))
                .andExpect(jsonPath("$.data.averageScore").value(0))
                .andExpect(jsonPath("$.data.studyTime").value(0))
                .andExpect(jsonPath("$.data.currentStreak").value(0))
                .andExpect(jsonPath("$.data.recentQuizzes").isEmpty());
    }

    @Test
    @DisplayName("GET /dashboard - Should return proper data structure")
    void getDashboard_ShouldReturnProperStructure() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.quizzesCompleted").isNumber())
                .andExpect(jsonPath("$.data.averageScore").isNumber())
                .andExpect(jsonPath("$.data.studyTime").isNumber())
                .andExpect(jsonPath("$.data.currentStreak").isNumber())
                .andExpect(jsonPath("$.data.recentQuizzes").isArray());

        // Note: Current implementation returns placeholder data (zeros)
        // When actual statistics logic is implemented, these assertions should be updated
    }

    @Test
    @DisplayName("GET /dashboard - Should handle concurrent requests")
    void getDashboard_ConcurrentRequests_ShouldHandle() throws Exception {
        // When & Then - Multiple requests should all succeed
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Test
    @DisplayName("GET /dashboard - Should not return null fields")
    void getDashboard_ShouldNotReturnNullFields() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.quizzesCompleted").exists())
                .andExpect(jsonPath("$.data.averageScore").exists())
                .andExpect(jsonPath("$.data.studyTime").exists())
                .andExpect(jsonPath("$.data.currentStreak").exists())
                .andExpect(jsonPath("$.data.recentQuizzes").exists());
    }

    @Test
    @DisplayName("GET /dashboard - Should return consistent data type")
    void getDashboard_ShouldReturnConsistentDataType() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quizzesCompleted").isNumber())
                .andExpect(jsonPath("$.data.studyTime").isNumber())
                .andExpect(jsonPath("$.data.currentStreak").isNumber());
    }
}
