package com.t3a.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.mapper.QuizSessionMapper;
import com.t3a.core.service.QuestionBankService;
import com.t3a.core.service.QuestionService;
import com.t3a.core.service.QuizSessionService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QuizController integration tests
 * Tests all endpoints of the QuizController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizSessionService sessionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionBankService bankService;

    @Autowired
    private QuizSessionMapper sessionMapper;

    private QuestionBank testBank;
    private QuizSession testSession;

    @BeforeEach
    void setUp() {
        // Create test question bank
        testBank = new QuestionBank();
        testBank.setName("Test Quiz Bank");
        testBank.setDescription("Bank for quiz testing");
        testBank.setCategory("Testing");
        testBank.setCreatorId(1L);
        testBank.setIsPublic(true);
        testBank.setAiGenerated(false);
        testBank.setDeleted(0);
        testBank = bankService.createBank(testBank);

        // Create test questions
        for (int i = 1; i <= 5; i++) {
            Question question = new Question();
            question.setBankId(testBank.getId());
            question.setContent("Test Question " + i);
            question.setQuestionType("SINGLE_CHOICE");
            question.setScore(10);
            question.setDifficulty("EASY");
            question.setDeleted(0);
            questionService.createQuestion(question);
        }
    }

    @Test
    @DisplayName("POST /quiz/start - Should start quiz successfully")
    void startQuiz_Success() throws Exception {
        // Given
        QuizController.StartQuizRequest request = new QuizController.StartQuizRequest();
        request.setUserId(1L);
        request.setBankId(testBank.getId());
        request.setQuestionCount(3);

        // When & Then
        mockMvc.perform(post("/quiz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.bankId").value(testBank.getId()))
                .andExpect(jsonPath("$.data.totalQuestions").value(3))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /quiz/start - Should fail with insufficient questions")
    void startQuiz_InsufficientQuestions_ShouldReturnError() throws Exception {
        // Given
        QuizController.StartQuizRequest request = new QuizController.StartQuizRequest();
        request.setUserId(1L);
        request.setBankId(testBank.getId());
        request.setQuestionCount(100); // More than available

        // When & Then
        mockMvc.perform(post("/quiz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("没有足够的题目")));
    }

    @Test
    @DisplayName("POST /quiz/start - Should fail with non-existent bank")
    void startQuiz_NonExistentBank_ShouldReturnError() throws Exception {
        // Given
        QuizController.StartQuizRequest request = new QuizController.StartQuizRequest();
        request.setUserId(1L);
        request.setBankId(999999L);
        request.setQuestionCount(3);

        // When & Then
        mockMvc.perform(post("/quiz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("POST /quiz/submit - Should submit quiz successfully")
    void submitQuiz_Success() throws Exception {
        // Given - Create a session first
        QuizSession session = sessionService.createSession(1L, testBank.getId(), 3);

        QuizController.SubmitQuizRequest request = new QuizController.SubmitQuizRequest();
        request.setSessionKey(session.getSessionKey());
        request.setUserScore(new BigDecimal("85.5"));

        // When & Then
        mockMvc.perform(post("/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").value(session.getSessionKey()))
                .andExpect(jsonPath("$.data.userScore").value(85.5))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /quiz/submit - Should fail with non-existent session")
    void submitQuiz_NonExistentSession_ShouldReturnError() throws Exception {
        // Given
        QuizController.SubmitQuizRequest request = new QuizController.SubmitQuizRequest();
        request.setSessionKey("non-existent-key");
        request.setUserScore(new BigDecimal("90.0"));

        // When & Then
        mockMvc.perform(post("/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("不存在")));
    }

    @Test
    @DisplayName("POST /quiz/submit - Should fail when session already completed")
    void submitQuiz_AlreadyCompleted_ShouldReturnError() throws Exception {
        // Given - Create and submit a session
        QuizSession session = sessionService.createSession(1L, testBank.getId(), 3);
        sessionService.submitSession(session.getSessionKey(), new BigDecimal("80.0"));

        QuizController.SubmitQuizRequest request = new QuizController.SubmitQuizRequest();
        request.setSessionKey(session.getSessionKey());
        request.setUserScore(new BigDecimal("90.0"));

        // When & Then
        mockMvc.perform(post("/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("已结束")));
    }

    @Test
    @DisplayName("GET /quiz/session/{sessionKey} - Should return session details")
    void getSession_ExistingSession_ShouldReturnSession() throws Exception {
        // Given
        QuizSession session = sessionService.createSession(1L, testBank.getId(), 3);

        // When & Then
        mockMvc.perform(get("/quiz/session/" + session.getSessionKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").value(session.getSessionKey()))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /quiz/session/{sessionKey} - Should return error for non-existent session")
    void getSession_NonExistentSession_ShouldReturnError() throws Exception {
        // When & Then
        mockMvc.perform(get("/quiz/session/non-existent-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("不存在")));
    }

    @Test
    @DisplayName("GET /quiz/history/{userId} - Should return user history")
    void getUserHistory_ShouldReturnSessions() throws Exception {
        // Given
        sessionService.createSession(1L, testBank.getId(), 3);
        sessionService.createSession(1L, testBank.getId(), 2);

        // When & Then
        mockMvc.perform(get("/quiz/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("GET /quiz/history/{userId} - Should return empty list for user with no sessions")
    void getUserHistory_NoSessions_ShouldReturnEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/quiz/history/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /quiz/result/{sessionKey} - Should return quiz result")
    void getQuizResult_CompletedSession_ShouldReturnResult() throws Exception {
        // Given
        QuizSession session = sessionService.createSession(1L, testBank.getId(), 3);
        sessionService.submitSession(session.getSessionKey(), new BigDecimal("85.5"));

        // When & Then
        mockMvc.perform(get("/quiz/result/" + session.getSessionKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").value(session.getSessionKey()))
                .andExpect(jsonPath("$.data.score").value(85))
                .andExpect(jsonPath("$.data.total").value(100))
                .andExpect(jsonPath("$.data.percentage").value(85))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /quiz/result/{sessionKey} - Should return result for in-progress session")
    void getQuizResult_InProgressSession_ShouldReturnPartialResult() throws Exception {
        // Given
        QuizSession session = sessionService.createSession(1L, testBank.getId(), 3);

        // When & Then
        mockMvc.perform(get("/quiz/result/" + session.getSessionKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").value(session.getSessionKey()))
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.percentage").value(0));
    }

    @Test
    @DisplayName("GET /quiz/result/{sessionKey} - Should return error for non-existent session")
    void getQuizResult_NonExistentSession_ShouldReturnError() throws Exception {
        // When & Then
        mockMvc.perform(get("/quiz/result/non-existent-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("不存在")));
    }

    @Test
    @DisplayName("Integration test - Complete quiz flow")
    void completeQuizFlow_ShouldWork() throws Exception {
        // 1. Start quiz
        String startResponse = mockMvc.perform(post("/quiz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createStartRequest(1L, testBank.getId(), 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionKey = objectMapper.readTree(startResponse)
                .path("data")
                .path("sessionKey")
                .asText();

        // 2. Get session details
        mockMvc.perform(get("/quiz/session/" + sessionKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 3. Submit quiz
        QuizController.SubmitQuizRequest submitRequest = new QuizController.SubmitQuizRequest();
        submitRequest.setSessionKey(sessionKey);
        submitRequest.setUserScore(new BigDecimal("92.5"));

        mockMvc.perform(post("/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.userScore").value(92.5));

        // 4. Get result
        mockMvc.perform(get("/quiz/result/" + sessionKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(92))
                .andExpect(jsonPath("$.data.percentage").value(92));

        // 5. Verify in history
        mockMvc.perform(get("/quiz/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /quiz/start - Should validate missing required fields")
    void startQuiz_MissingRequiredFields_ShouldReturnError() throws Exception {
        // Given - Missing bankId
        QuizController.StartQuizRequest request = new QuizController.StartQuizRequest();
        request.setUserId(1L);
        request.setQuestionCount(3);
        // bankId is null

        // When & Then - Should still work or return validation error depending on implementation
        mockMvc.perform(post("/quiz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500)); // Expected to fail due to null bankId
    }

    // Helper method
    private QuizController.StartQuizRequest createStartRequest(Long userId, Long bankId, Integer questionCount) {
        QuizController.StartQuizRequest request = new QuizController.StartQuizRequest();
        request.setUserId(userId);
        request.setBankId(bankId);
        request.setQuestionCount(questionCount);
        return request;
    }
}
