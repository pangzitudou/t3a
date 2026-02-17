package com.t3a.core.service;

import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuestionBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 题目服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionBankService bankService;

    private Long bankId;

    @BeforeEach
    void setUp() {
        // 创建测试题库
        QuestionBank bank = new QuestionBank();
        bank.setName("测试题库");
        bank.setDescription("用于测试的题库");
        bank.setCategory("Test");
        bank.setCreatorId(1L);
        bank.setIsPublic(true);
        bank.setAiGenerated(false);

        QuestionBank created = bankService.createBank(bank);
        bankId = created.getId();
    }

    @Test
    void testCreateQuestion_Success() {
        // 创建题目
        Question question = createTestQuestion("单选题测试", "SINGLE_CHOICE");
        Question created = questionService.createQuestion(question);

        // 验证
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("单选题测试", created.getContent());
        assertEquals("SINGLE_CHOICE", created.getQuestionType());
    }

    @Test
    void testBatchCreate_Success() {
        // 创建多个题目
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            questions.add(createTestQuestion("题目" + i, "SINGLE_CHOICE"));
        }

        questionService.batchCreate(questions);

        // 验证
        List<Question> savedQuestions = questionService.listByBankId(bankId);
        assertEquals(10, savedQuestions.size());
    }

    @Test
    void testGetRandomQuestions_Success() {
        // 先创建20道题
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            questions.add(createTestQuestion("题目" + i, "SINGLE_CHOICE"));
        }
        questionService.batchCreate(questions);

        // 随机获取10道题
        List<Question> randomQuestions = questionService.getRandomQuestions(bankId, 10);

        // 验证
        assertNotNull(randomQuestions);
        assertEquals(10, randomQuestions.size());
    }

    @Test
    void testGetRandomQuestions_LessThanAvailable() {
        // 只创建5道题
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(createTestQuestion("题目" + i, "SINGLE_CHOICE"));
        }
        questionService.batchCreate(questions);

        // 请求10道题
        List<Question> randomQuestions = questionService.getRandomQuestions(bankId, 10);

        // 应该返回所有5道题
        assertEquals(5, randomQuestions.size());
    }

    @Test
    void testGetRandomQuestionsByDifficulty_Success() {
        // 创建不同难度的题目
        for (int i = 1; i <= 5; i++) {
            Question q = createTestQuestion("简单题" + i, "SINGLE_CHOICE");
            q.setDifficulty("EASY");
            questionService.createQuestion(q);
        }

        for (int i = 1; i <= 10; i++) {
            Question q = createTestQuestion("中等题" + i, "SINGLE_CHOICE");
            q.setDifficulty("MEDIUM");
            questionService.createQuestion(q);
        }

        // 随机获取3道中等难度的题
        List<Question> mediumQuestions = questionService.getRandomQuestionsByDifficulty(
                bankId, "MEDIUM", 3
        );

        // 验证
        assertEquals(3, mediumQuestions.size());
        mediumQuestions.forEach(q -> assertEquals("MEDIUM", q.getDifficulty()));
    }

    @Test
    void testCountByBankId_Success() {
        // 创建10道题
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            questions.add(createTestQuestion("题目" + i, "SINGLE_CHOICE"));
        }
        questionService.batchCreate(questions);

        // 统计数量
        Long count = questionService.countByBankId(bankId);
        assertEquals(10L, count);
    }

    @Test
    void testDeleteQuestion_Success() {
        // 创建题目
        Question question = createTestQuestion("待删除题目", "SINGLE_CHOICE");
        Question created = questionService.createQuestion(question);

        // 删除
        questionService.deleteQuestion(created.getId());

        // 验证已删除
        Question deleted = questionService.getById(created.getId());
        assertNull(deleted);
    }

    /**
     * 创建测试题目
     */
    private Question createTestQuestion(String content, String type) {
        Question question = new Question();
        question.setBankId(bankId);
        question.setQuestionType(type);
        question.setContent(content);
        question.setOptions("[\"A\", \"B\", \"C\", \"D\"]");
        question.setCorrectAnswer("A");
        question.setExplanation("这是解析");
        question.setDifficulty("MEDIUM");
        question.setTags("测试,单元测试");
        question.setScore(10);
        question.setAiGenerated(false);
        return question;
    }
}
