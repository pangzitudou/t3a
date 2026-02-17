package com.t3a.core.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.mapper.QuizSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.t3a.common.constant.QuizConstants.REDIS_KEY_QUIZ_SESSION;

/**
 * 测验会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionService {

    private final QuizSessionMapper sessionMapper;
    private final QuestionService questionService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 创建测验会话
     *
     * @param userId 用户ID
     * @param bankId 题库ID
     * @param questionCount 题目数量
     * @return 测验会话
     */
    @Transactional(rollbackFor = Exception.class)
    public QuizSession createSession(Long userId, Long bankId, Integer questionCount) {
        log.info("创建测验会话: userId={}, bankId={}, questionCount={}", userId, bankId, questionCount);

        // 随机选择题目
        List<Question> questions = questionService.getRandomQuestions(bankId, questionCount);

        if (questions.isEmpty()) {
            throw new RuntimeException("题库中没有足够的题目");
        }

        // 计算总分
        int totalScore = questions.stream()
                .mapToInt(q -> q.getScore() != null ? q.getScore() : 10)
                .sum();

        // 创建会话
        QuizSession session = new QuizSession();
        session.setSessionKey(IdUtil.simpleUUID());
        session.setUserId(userId);
        session.setBankId(bankId);
        session.setTotalQuestions(questions.size());
        session.setAnsweredCount(0);
        session.setTotalScore(totalScore);
        session.setUserScore(BigDecimal.ZERO);
        session.setStatus("IN_PROGRESS");
        session.setStartTime(LocalDateTime.now());
        session.setDeleted(0);

        sessionMapper.insert(session);

        // 缓存会话到 Redis（1小时过期）
        cacheSession(session);

        log.info("测验会话创建成功: sessionKey={}", session.getSessionKey());
        return session;
    }

    /**
     * 提交测验
     */
    @Transactional(rollbackFor = Exception.class)
    public QuizSession submitSession(String sessionKey, BigDecimal userScore) {
        log.info("提交测验: sessionKey={}, score={}", sessionKey, userScore);

        QuizSession session = getBySessionKey(sessionKey);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("会话已结束");
        }

        // 更新会话
        session.setUserScore(userScore);
        session.setStatus("COMPLETED");
        session.setSubmitTime(LocalDateTime.now());

        // 计算耗时
        if (session.getStartTime() != null) {
            Duration duration = Duration.between(session.getStartTime(), LocalDateTime.now());
            session.setTimeSpent((int) duration.getSeconds());
        }

        sessionMapper.updateById(session);

        // 更新缓存
        cacheSession(session);

        log.info("测验提交成功: sessionKey={}, timeSpent={}s", sessionKey, session.getTimeSpent());
        return session;
    }

    /**
     * 根据会话Key查询会话
     */
    public QuizSession getBySessionKey(String sessionKey) {
        // 先从缓存查询
        QuizSession cached = getSessionFromCache(sessionKey);
        if (cached != null) {
            return cached;
        }

        // 从数据库查询
        QuizSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<QuizSession>()
                        .eq(QuizSession::getSessionKey, sessionKey)
                        .eq(QuizSession::getDeleted, 0)
        );

        if (session != null) {
            cacheSession(session);
        }

        return session;
    }

    /**
     * 查询用户的所有会话
     */
    public List<QuizSession> listUserSessions(Long userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<QuizSession>()
                        .eq(QuizSession::getUserId, userId)
                        .eq(QuizSession::getDeleted, 0)
                        .orderByDesc(QuizSession::getCreateTime)
        );
    }

    /**
     * 缓存会话到Redis
     */
    private void cacheSession(QuizSession session) {
        String key = REDIS_KEY_QUIZ_SESSION + session.getSessionKey();
        // 这里简化处理，实际应该序列化整个对象
        redisTemplate.opsForValue().set(
                key,
                session.getId().toString(),
                1,
                TimeUnit.HOURS
        );
    }

    /**
     * 从Redis获取会话
     */
    private QuizSession getSessionFromCache(String sessionKey) {
        String key = REDIS_KEY_QUIZ_SESSION + sessionKey;
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            return sessionMapper.selectById(Long.parseLong(value));
        }

        return null;
    }
}
