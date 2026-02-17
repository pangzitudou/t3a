package com.t3a.core.service;

import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.User;
import com.t3a.core.domain.entity.UserAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizSessionServiceTest {

    @Autowired
    private QuizSessionService sessionService;

    @Autowired
    private QuestionBankService bankService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserService userService;

    private Long userId;
    private Long bankId;
    private Long singleQuestionId;
    private Long multipleQuestionId;
    private Long shortQuestionId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("testuser" + System.currentTimeMillis());
        user.setPassword("test123");
        user.setNickname("测试用户");
        user.setEmail("test@test.com");
        User createdUser = userService.createUser(user);
        userId = createdUser.getId();

        QuestionBank bank = new QuestionBank();
        bank.setName("测试题库");
        bank.setDescription("用于测试");
        bank.setCategory("Test");
        bank.setCreatorId(userId);
        bank.setIsPublic(true);
        bank.setAiGenerated(false);
        QuestionBank createdBank = bankService.createBank(bank);
        bankId = createdBank.getId();

        Question single = new Question();
        single.setBankId(bankId);
        single.setQuestionType("SINGLE_CHOICE");
        single.setContent("单选题");
        single.setOptions("[\"A\",\"B\",\"C\",\"D\"]");
        single.setCorrectAnswer("A");
        single.setExplanation("单选解析");
        single.setDifficulty("MEDIUM");
        single.setScore(10);
        single.setAiGenerated(false);
        singleQuestionId = questionService.createQuestion(single).getId();

        Question multi = new Question();
        multi.setBankId(bankId);
        multi.setQuestionType("MULTIPLE_CHOICE");
        multi.setContent("多选题");
        multi.setOptions("[\"A\",\"B\",\"C\",\"D\"]");
        multi.setCorrectAnswer("A,C");
        multi.setExplanation("多选解析");
        multi.setDifficulty("MEDIUM");
        multi.setScore(10);
        multi.setAiGenerated(false);
        multipleQuestionId = questionService.createQuestion(multi).getId();

        Question shortQ = new Question();
        shortQ.setBankId(bankId);
        shortQ.setQuestionType("SHORT_ANSWER");
        shortQ.setContent("简述 Redis 单线程优势");
        shortQ.setCorrectAnswer("高并发 网络IO 非阻塞");
        shortQ.setExplanation("要点：高并发、网络IO、多路复用");
        shortQ.setDifficulty("MEDIUM");
        shortQ.setScore(10);
        shortQ.setAiGenerated(false);
        shortQuestionId = questionService.createQuestion(shortQ).getId();

        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Question q = new Question();
            q.setBankId(bankId);
            q.setQuestionType("SINGLE_CHOICE");
            q.setContent("测试题目 " + i);
            q.setOptions("[\"A\", \"B\", \"C\", \"D\"]");
            q.setCorrectAnswer("A");
            q.setExplanation("解析");
            q.setDifficulty("MEDIUM");
            q.setScore(10);
            q.setAiGenerated(false);
            questions.add(q);
        }
        questionService.batchCreate(questions);
    }

    @Test
    void testCreateSession_Success() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        assertNotNull(session);
        assertNotNull(session.getId());
        assertNotNull(session.getSessionKey());
        assertEquals(userId, session.getUserId());
        assertEquals(bankId, session.getBankId());
        assertEquals(10, session.getTotalQuestions());
        assertEquals(0, session.getAnsweredCount());
        assertEquals(100, session.getTotalScore());
        assertEquals("IN_PROGRESS", session.getStatus());
        assertNotNull(session.getStartTime());
    }

    @Test
    void testSaveAnswer_SingleChoiceAndMultipleChoice_ShouldScoreCorrectly() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);

        sessionService.saveAnswer(session.getSessionKey(), singleQuestionId, 0);
        sessionService.saveAnswer(session.getSessionKey(), multipleQuestionId, List.of(0, 2));

        List<UserAnswer> answers = sessionService.listAnswers(session.getId());
        assertEquals(2, answers.size());
        assertEquals(2, sessionService.getBySessionKey(session.getSessionKey()).getAnsweredCount());

        BigDecimal sum = answers.stream()
                .map(a -> a.getScore() == null ? BigDecimal.ZERO : a.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("20.00"), sum.setScale(2));
    }

    @Test
    void testSaveAnswer_MultipleChoiceWrong_ShouldScoreZero() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);

        sessionService.saveAnswer(session.getSessionKey(), multipleQuestionId, List.of(0));
        List<UserAnswer> answers = sessionService.listAnswers(session.getId());

        assertEquals(1, answers.size());
        assertEquals(0, answers.get(0).getScore().intValue());
        assertEquals(0, answers.get(0).getIsCorrect());
    }

    @Test
    void testSubmitSession_ShouldUseServerCalculatedScore() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        sessionService.saveAnswer(session.getSessionKey(), singleQuestionId, 0);
        sessionService.saveAnswer(session.getSessionKey(), multipleQuestionId, List.of(0, 2));

        QuizSession submitted = sessionService.submitSession(session.getSessionKey(), new BigDecimal("1.00"));
        assertEquals("COMPLETED", submitted.getStatus());
        assertEquals(new BigDecimal("20.00"), submitted.getUserScore().setScale(2));
        assertNotNull(submitted.getSubmitTime());
    }

    @Test
    void testSaveAnswer_ShortAnswer_ShouldSupportPartialScore() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        sessionService.saveAnswer(session.getSessionKey(), shortQuestionId, "Redis 高并发，依靠网络IO模型");

        List<UserAnswer> answers = sessionService.listAnswers(session.getId());
        assertEquals(1, answers.size());
        assertTrue(answers.get(0).getScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(answers.get(0).getScore().compareTo(new BigDecimal("10.00")) < 0);
    }

    @Test
    void testSubmitSession_AlreadyCompleted() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        sessionService.submitSession(session.getSessionKey(), new BigDecimal("80"));

        assertThrows(IllegalStateException.class, () ->
                sessionService.submitSession(session.getSessionKey(), new BigDecimal("90")));
    }

    @Test
    void testGetBySessionKey_Success() {
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        QuizSession found = sessionService.getBySessionKey(session.getSessionKey());
        assertNotNull(found);
        assertEquals(session.getId(), found.getId());
        assertEquals(session.getSessionKey(), found.getSessionKey());
    }

    @Test
    void testListUserSessions_Success() {
        sessionService.createSession(userId, bankId, 10);
        sessionService.createSession(userId, bankId, 8);
        sessionService.createSession(userId, bankId, 6);

        List<QuizSession> sessions = sessionService.listUserSessions(userId);
        assertNotNull(sessions);
        assertEquals(3, sessions.size());
    }
}
