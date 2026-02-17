package com.t3a.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 题目服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final Random random = new Random();

    /**
     * 创建题目
     */
    @Transactional(rollbackFor = Exception.class)
    public Question createQuestion(Question question) {
        log.info("创建题目: bankId={}, type={}", question.getBankId(), question.getQuestionType());
        question.setDeleted(0);
        questionMapper.insert(question);
        return question;
    }

    /**
     * 批量创建题目
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(List<Question> questions) {
        log.info("批量创建题目: count={}", questions.size());
        if (questions == null || questions.isEmpty()) {
            return;
        }

        // 输入批次内去重（同题库 + 题干）
        Map<String, Question> dedupedInput = new LinkedHashMap<>();
        for (Question q : questions) {
            if (q == null || q.getBankId() == null) {
                continue;
            }
            String key = q.getBankId() + "#" + normalizeContent(q.getContent());
            if (!dedupedInput.containsKey(key)) {
                dedupedInput.put(key, q);
            }
        }

        // 与数据库现有题目去重（同题库 + 题干）
        List<Question> toInsert = new ArrayList<>();
        for (Question q : dedupedInput.values()) {
            String normalized = normalizeContent(q.getContent());
            long exists = questionMapper.selectCount(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getBankId, q.getBankId())
                            .eq(Question::getDeleted, 0)
                            .apply("LOWER(TRIM(content)) = {0}", normalized)
            );
            if (exists == 0) {
                q.setDeleted(0);
                toInsert.add(q);
            }
        }

        toInsert.forEach(questionMapper::insert);
        log.info("批量创建题目完成: request={}, inserted={}, deduped={}",
                questions.size(), toInsert.size(), questions.size() - toInsert.size());
    }

    /**
     * 查询题库下的所有题目
     */
    public List<Question> listByBankId(Long bankId) {
        return questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getBankId, bankId)
                        .eq(Question::getDeleted, 0)
        );
    }

    /**
     * 随机获取题目
     *
     * @param bankId 题库ID
     * @param count 题目数量
     * @return 随机题目列表
     */
    public List<Question> getRandomQuestions(Long bankId, Integer count) {
        log.info("随机获取题目: bankId={}, count={}", bankId, count);

        // 查询所有题目
        List<Question> allQuestions = deduplicateQuestions(listByBankId(bankId));

        if (allQuestions.isEmpty()) {
            log.warn("题库为空: bankId={}", bankId);
            return Collections.emptyList();
        }

        // 如果请求数量大于总数，返回所有题目
        if (count >= allQuestions.size()) {
            Collections.shuffle(allQuestions);
            return allQuestions;
        }

        // 随机选择
        List<Question> selectedQuestions = new ArrayList<>();
        Collections.shuffle(allQuestions, random);
        for (int i = 0; i < count && i < allQuestions.size(); i++) {
            selectedQuestions.add(allQuestions.get(i));
        }

        return selectedQuestions;
    }

    /**
     * 按难度随机获取题目
     */
    public List<Question> getRandomQuestionsByDifficulty(Long bankId, String difficulty, Integer count) {
        log.info("按难度随机获取题目: bankId={}, difficulty={}, count={}", bankId, difficulty, count);

        List<Question> questions = deduplicateQuestions(questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getBankId, bankId)
                        .eq(Question::getDifficulty, difficulty)
                        .eq(Question::getDeleted, 0)
        ));

        if (questions.size() <= count) {
            Collections.shuffle(questions);
            return questions;
        }

        Collections.shuffle(questions, random);
        return questions.subList(0, count);
    }

    /**
     * 根据ID查询题目
     */
    public Question getById(Long id) {
        return questionMapper.selectOne(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getId, id)
                        .eq(Question::getDeleted, 0)
        );
    }

    /**
     * 更新题目
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(Question question) {
        log.info("更新题目: id={}", question.getId());
        questionMapper.updateById(question);
    }

    /**
     * 删除题目（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long id) {
        log.info("删除题目: id={}", id);
        // Leverage MyBatis-Plus logic delete to avoid generating empty UPDATE SET SQL.
        questionMapper.deleteById(id);
    }

    /**
     * 统计题库的题目数量
     */
    public Long countByBankId(Long bankId) {
        return questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getBankId, bankId)
                        .eq(Question::getDeleted, 0)
        );
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private List<Question> deduplicateQuestions(List<Question> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        Map<String, Question> deduped = new LinkedHashMap<>();
        for (Question q : source) {
            String key = normalizeContent(q.getContent());
            if (!deduped.containsKey(key)) {
                deduped.put(key, q);
            }
        }
        return new ArrayList<>(deduped.values());
    }
}
