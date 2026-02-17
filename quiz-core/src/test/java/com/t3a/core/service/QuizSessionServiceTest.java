package com.t3a.core.service;

import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.User;
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

/**
 * 测验会话服务测试
 */
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

    @BeforeEach
    void setUp() {
        // 创建测试用户
        User user = new User();
        user.setUsername("testuser" + System.currentTimeMillis());
        user.setPassword("test123");
        user.setNickname("测试用户");
        user.setEmail("test@test.com");
        User createdUser = userService.createUser(user);
        userId = createdUser.getId();

        // 创建测试题库
        QuestionBank bank = new QuestionBank();
        bank.setName("测试题库");
        bank.setDescription("用于测试");
        bank.setCategory("Test");
        bank.setCreatorId(userId);
        bank.setIsPublic(true);
        bank.setAiGenerated(false);
        QuestionBank createdBank = bankService.createBank(bank);
        bankId = createdBank.getId();

        // 创建20道测试题目
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
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
        // 创建会话
        QuizSession session = sessionService.createSession(userId, bankId, 10);

        // 验证
        assertNotNull(session);
        assertNotNull(session.getId());
        assertNotNull(session.getSessionKey());
        assertEquals(userId, session.getUserId());
        assertEquals(bankId, session.getBankId());
        assertEquals(10, session.getTotalQuestions());
        assertEquals(0, session.getAnsweredCount());
        assertEquals(100, session.getTotalScore()); // 10题 * 10分
        assertEquals("IN_PROGRESS", session.getStatus());
        assertNotNull(session.getStartTime());
    }

    @Test
    void testCreateSession_InsufficientQuestions() {
        // 创建新题库（无题目）
        QuestionBank emptyBank = new QuestionBank();
        emptyBank.setName("空题库");
        emptyBank.setDescription("无题目");
        emptyBank.setCategory("Test");
        emptyBank.setCreatorId(userId);
        emptyBank.setIsPublic(false);
        emptyBank.setAiGenerated(false);
        QuestionBank created = bankService.createBank(emptyBank);

        // 尝试创建会话应该失败
        assertThrows(RuntimeException.class, () -> {
            sessionService.createSession(userId, created.getId(), 10);
        });
    }

    @Test
    void testSubmitSession_Success() throws InterruptedException {
        // 创建会话
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        String sessionKey = session.getSessionKey();

        // 等待1秒以确保有耗时
        Thread.sleep(1000);

        // 提交会话
        BigDecimal userScore = new BigDecimal("85.5");
        QuizSession submitted = sessionService.submitSession(sessionKey, userScore);

        // 验证
        assertNotNull(submitted);
        assertEquals("COMPLETED", submitted.getStatus());
        assertEquals(userScore, submitted.getUserScore());
        assertNotNull(submitted.getSubmitTime());
        assertTrue(submitted.getTimeSpent() >= 1); // 至少1秒
    }

    @Test
    void testSubmitSession_AlreadyCompleted() {
        // 创建并提交会话
        QuizSession session = sessionService.createSession(userId, bankId, 10);
        sessionService.submitSession(session.getSessionKey(), new BigDecimal("80"));

        // 再次提交应该失败
        assertThrows(IllegalStateException.class, () -> {
            sessionService.submitSession(session.getSessionKey(), new BigDecimal("90"));
        });
    }

    @Test
    void testGetBySessionKey_Success() {
        // 创建会话
        QuizSession session = sessionService.createSession(userId, bankId, 10);

        // 通过sessionKey查询
        QuizSession found = sessionService.getBySessionKey(session.getSessionKey());

        // 验证
        assertNotNull(found);
        assertEquals(session.getId(), found.getId());
        assertEquals(session.getSessionKey(), found.getSessionKey());
    }

    @Test
    void testListUserSessions_Success() {
        // 创建多个会话
        sessionService.createSession(userId, bankId, 10);
        sessionService.createSession(userId, bankId, 15);
        sessionService.createSession(userId, bankId, 20);

        // 查询用户的所有会话
        List<QuizSession> sessions = sessionService.listUserSessions(userId);

        // 验证
        assertNotNull(sessions);
        assertEquals(3, sessions.size());
    }
}
