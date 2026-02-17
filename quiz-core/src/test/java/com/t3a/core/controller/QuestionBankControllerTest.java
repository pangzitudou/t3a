package com.t3a.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.mapper.QuestionBankMapper;
import com.t3a.core.service.QuestionBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QuestionBank Controller integration tests
 * Tests all endpoints of the QuestionBankController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionBankService bankService;

    @Autowired
    private QuestionBankMapper bankMapper;

    private QuestionBank testBank;

    @BeforeEach
    void setUp() {
        // Prepare test data
        testBank = new QuestionBank();
        testBank.setName("Test Bank");
        testBank.setDescription("Test Description");
        testBank.setCategory("Test Category");
        testBank.setCreatorId(1L);
        testBank.setIsPublic(true);
        testBank.setAiGenerated(false);
        testBank.setDeleted(0);
    }

    @Test
    @DisplayName("GET /bank/list - Should return paginated list")
    void listBanks_ShouldReturnPage() throws Exception {
        // Given - create a test bank
        bankService.createBank(testBank);

        // When & Then
        mockMvc.perform(get("/bank/list")
                        .with(anonymous())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("GET /bank/list - Should work with default parameters")
    void listBanks_DefaultParameters_ShouldWork() throws Exception {
        // Test without providing pageNum and pageSize (using defaults)
        mockMvc.perform(get("/bank/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("GET /bank/list - Should filter by creatorId")
    void listBanks_WithCreatorId_ShouldFilter() throws Exception {
        // Given
        testBank.setCreatorId(100L);
        bankService.createBank(testBank);

        // When & Then
        mockMvc.perform(get("/bank/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("creatorId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /bank/list - Should filter by category")
    void listBanks_WithCategory_ShouldFilter() throws Exception {
        // Given
        testBank.setCategory("Java");
        bankService.createBank(testBank);

        // When & Then
        mockMvc.perform(get("/bank/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("category", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("POST /bank/create - Should create new bank")
    void createBank_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/bank/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBank)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Bank"))
                .andExpect(jsonPath("$.data.description").value("Test Description"))
                .andExpect(jsonPath("$.data.category").value("Test Category"));
    }

    @Test
    @DisplayName("POST /bank/create - Should fail with empty name")
    void createBank_EmptyName_ShouldReturnError() throws Exception {
        // Given
        testBank.setName("");

        // When & Then
        mockMvc.perform(post("/bank/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBank)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /bank/{id} - Should return bank by ID")
    void getBank_ExistingId_ShouldReturnBank() throws Exception {
        // Given
        QuestionBank created = bankService.createBank(testBank);

        // When & Then
        mockMvc.perform(get("/bank/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(created.getId()))
                .andExpect(jsonPath("$.data.name").value("Test Bank"));
    }

    @Test
    @DisplayName("GET /bank/{id} - Should return error for non-existent ID")
    void getBank_NonExistentId_ShouldReturnError() throws Exception {
        // When & Then
        mockMvc.perform(get("/bank/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("不存在")));
    }

    @Test
    @DisplayName("GET /bank/public - Should return public banks")
    void listPublicBanks_ShouldReturnPublicBanks() throws Exception {
        // Given
        testBank.setIsPublic(true);
        bankService.createBank(testBank);

        QuestionBank privateBank = new QuestionBank();
        privateBank.setName("Private Bank");
        privateBank.setIsPublic(false);
        privateBank.setCreatorId(2L);
        privateBank.setDeleted(0);
        bankService.createBank(privateBank);

        // When & Then
        mockMvc.perform(get("/bank/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("PUT /bank/update - Should update existing bank")
    void updateBank_Success() throws Exception {
        // Given
        QuestionBank created = bankService.createBank(testBank);
        created.setName("Updated Bank Name");
        created.setDescription("Updated Description");

        // When & Then
        mockMvc.perform(put("/bank/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("成功")));
    }

    @Test
    @DisplayName("PUT /bank/update - Should handle non-existent bank gracefully")
    void updateBank_NonExistentId_ShouldStillReturnSuccess() throws Exception {
        // Given
        testBank.setId(999999L);

        // When & Then
        mockMvc.perform(put("/bank/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBank)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /bank/{id} - Should delete bank")
    void deleteBank_Success() throws Exception {
        // Given
        QuestionBank created = bankService.createBank(testBank);

        // When & Then
        mockMvc.perform(delete("/bank/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("删除成功")));

        // Verify - bank should be logically deleted
        QuestionBank deleted = bankService.getById(created.getId());
        assertNull(deleted);
    }

    @Test
    @DisplayName("DELETE /bank/{id} - Should handle non-existent bank gracefully")
    void deleteBank_NonExistentId_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/bank/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(containsString("删除成功")));
    }

    @Test
    @DisplayName("Integration test - Create, Read, Update, Delete flow")
    void fullCrudFlow_ShouldWork() throws Exception {
        // 1. Create
        String createResponse = mockMvc.perform(post("/bank/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBank)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract ID from response
        Long bankId = objectMapper.readTree(createResponse)
                .path("data")
                .path("id")
                .asLong();

        // 2. Read
        mockMvc.perform(get("/bank/" + bankId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bankId));

        // 3. Update
        testBank.setId(bankId);
        testBank.setName("Updated Name");
        mockMvc.perform(put("/bank/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBank)))
                .andExpect(status().isOk());

        // Verify update
        mockMvc.perform(get("/bank/" + bankId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        // 4. Delete
        mockMvc.perform(delete("/bank/" + bankId))
                .andExpect(status().isOk());

        // Verify deletion
        mockMvc.perform(get("/bank/" + bankId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
