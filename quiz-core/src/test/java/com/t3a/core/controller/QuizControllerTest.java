package com.t3a.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.User;
import com.t3a.core.service.QuestionBankService;
import com.t3a.core.service.QuestionService;
import com.t3a.core.service.QuizSessionService;
import com.t3a.core.service.UserService;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
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
    private UserService userService;

    private QuestionBank testBank;
    private Long singleQuestionId;
    private Long multipleQuestionId;
    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("quiz-controller-" + System.currentTimeMillis());
        user.setPassword("123456");
        user.setEmail("quiz-controller@test.com");
        user.setNickname("controller");
        userId = userService.createUser(user).getId();

        testBank = new QuestionBank();
        testBank.setName("Test Quiz Bank");
        testBank.setDescription("Bank for quiz testing");
        testBank.setCategory("Testing");
        testBank.setCreatorId(userId);
        testBank.setIsPublic(true);
        testBank.setAiGenerated(false);
        testBank.setDeleted(0);
        testBank = bankService.createBank(testBank);

        Question single = new Question();
        single.setBankId(testBank.getId());
        single.setContent("Single Question");
        single.setQuestionType("SINGLE_CHOICE");
        single.setScore(10);
        single.setOptions("[\"A\",\"B\",\"C\",\"D\"]");
        single.setCorrectAnswer("A");
        single.setExplanation("single exp");
        single.setDifficulty("EASY");
        single.setDeleted(0);
        singleQuestionId = questionService.createQuestion(single).getId();

        Question multiple = new Question();
        multiple.setBankId(testBank.getId());
        multiple.setContent("Multiple Question");
        multiple.setQuestionType("MULTIPLE_CHOICE");
        multiple.setScore(10);
        multiple.setOptions("[\"A\",\"B\",\"C\",\"D\"]");
        multiple.setCorrectAnswer("A,C");
        multiple.setExplanation("multi exp");
        multiple.setDifficulty("EASY");
        multiple.setDeleted(0);
        multipleQuestionId = questionService.createQuestion(multiple).getId();

        for (int i = 1; i <= 3; i++) {
            Question q = new Question();
            q.setBankId(testBank.getId());
            q.setContent("Extra Question " + i);
            q.setQuestionType("SINGLE_CHOICE");
            q.setScore(10);
            q.setOptions("[\"A\",\"B\",\"C\",\"D\"]");
            q.setCorrectAnswer("A");
            q.setDifficulty("EASY");
            q.setDeleted(0);
            questionService.createQuestion(q);
        }
    }

    @Test
    @DisplayName("POST /quiz/session/{sessionKey}/answer - single & multiple should be scored")
    void submitAnswer_ShouldScore() throws Exception {
        QuizSession session = sessionService.createSession(userId, testBank.getId(), 5);

        QuizController.AnswerRequest singleReq = new QuizController.AnswerRequest();
        singleReq.setQuestionId(singleQuestionId);
        singleReq.setAnswer(0);

        mockMvc.perform(post("/session/" + session.getSessionKey() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answeredCount").value(1));

        QuizController.AnswerRequest multipleReq = new QuizController.AnswerRequest();
        multipleReq.setQuestionId(multipleQuestionId);
        multipleReq.setAnswer(List.of(0, 2));

        mockMvc.perform(post("/session/" + session.getSessionKey() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multipleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answeredCount").value(2));
    }

    @Test
    @DisplayName("POST /quiz/submit - should use server score from answers")
    void submitQuiz_ShouldUseServerScore() throws Exception {
        QuizSession session = sessionService.createSession(userId, testBank.getId(), 5);
        sessionService.saveAnswer(session.getSessionKey(), singleQuestionId, 0);
        sessionService.saveAnswer(session.getSessionKey(), multipleQuestionId, List.of(0, 2));

        QuizController.SubmitQuizRequest request = new QuizController.SubmitQuizRequest();
        request.setSessionKey(session.getSessionKey());
        request.setUserScore(new java.math.BigDecimal("99.5"));

        mockMvc.perform(post("/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.userScore").value(20));
    }

    @Test
    @DisplayName("GET /quiz/result/{sessionKey} - should return answer details")
    void getQuizResult_ShouldReturnAnswerDetails() throws Exception {
        QuizSession session = sessionService.createSession(userId, testBank.getId(), 5);
        sessionService.saveAnswer(session.getSessionKey(), singleQuestionId, 0);
        sessionService.saveAnswer(session.getSessionKey(), multipleQuestionId, List.of(0, 2));
        sessionService.submitSession(session.getSessionKey(), java.math.BigDecimal.ZERO);

        mockMvc.perform(get("/result/" + session.getSessionKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionKey").value(session.getSessionKey()))
                .andExpect(jsonPath("$.data.score").value(20))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.data.totalQuestions").value(5))
                .andExpect(jsonPath("$.data.correctAnswers").value(2))
                .andExpect(jsonPath("$.data.answers", hasSize(2)))
                .andExpect(jsonPath("$.data.answers[0].question").isNotEmpty())
                .andExpect(jsonPath("$.data.answers[0].correctAnswer").isNotEmpty())
                .andExpect(jsonPath("$.data.answers[0].explanation").isNotEmpty());
    }

    @Test
    @DisplayName("POST /quiz/start - Should fail with insufficient questions")
    void startQuiz_InsufficientQuestions_ShouldReturnError() throws Exception {
        QuizController.StartQuizRequest request = new QuizController.StartQuizRequest();
        request.setUserId(userId);
        request.setBankId(testBank.getId());
        request.setQuestionCount(100);

        mockMvc.perform(post("/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("没有足够的题目")));
    }
}
