package com.t3a.core.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.UserAnswer;
import com.t3a.core.mapper.QuizSessionMapper;
import com.t3a.core.mapper.UserAnswerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final UserAnswerMapper userAnswerMapper;
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
        int requiredCount = questionCount == null ? 10 : questionCount;
        if (requiredCount <= 0) {
            throw new IllegalArgumentException("题目数量必须大于0");
        }

        // 随机选择题目
        List<Question> questions = questionService.getRandomQuestions(bankId, requiredCount);

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("题库中没有可用题目");
        }

        if (questions.size() < requiredCount) {
            throw new IllegalArgumentException(
                    String.format("题库中没有足够的题目：当前%d题，至少需要%d题", questions.size(), requiredCount)
            );
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

        BigDecimal calculated = calculateSessionScore(session.getId());

        // 更新会话，优先使用服务端计算分数，避免前端未传分导致 0 分
        session.setUserScore(calculated != null ? calculated : (userScore == null ? BigDecimal.ZERO : userScore));
        session.setAnsweredCount(countAnswered(session.getId()));
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
     * 保存用户答案并实时判分
     */
    @Transactional(rollbackFor = Exception.class)
    public QuizSession saveAnswer(String sessionKey, Long questionId, Object answer) {
        QuizSession session = getBySessionKey(sessionKey);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("会话已结束");
        }

        Question question = questionService.getById(questionId);
        if (question == null || !Objects.equals(question.getBankId(), session.getBankId())) {
            throw new IllegalArgumentException("题目不存在或不属于当前会话题库");
        }

        String userAnswerText = normalizeAnswerValue(answer);
        Evaluation evaluation = evaluateAnswer(question, answer);
        boolean correct = evaluation.correct();
        BigDecimal score = evaluation.score();

        UserAnswer existing = userAnswerMapper.selectOne(
                new LambdaQueryWrapper<UserAnswer>()
                        .eq(UserAnswer::getSessionId, session.getId())
                        .eq(UserAnswer::getQuestionId, questionId)
                        .eq(UserAnswer::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            UserAnswer record = new UserAnswer();
            record.setSessionId(session.getId());
            record.setQuestionId(questionId);
            record.setUserAnswer(userAnswerText);
            record.setIsCorrect(correct ? 1 : 0);
            record.setScore(score);
            record.setAiFeedback(evaluation.feedback());
            record.setAnswerTime(LocalDateTime.now());
            record.setDeleted(0);
            userAnswerMapper.insert(record);
        } else {
            existing.setUserAnswer(userAnswerText);
            existing.setIsCorrect(correct ? 1 : 0);
            existing.setScore(score);
            existing.setAiFeedback(evaluation.feedback());
            existing.setAnswerTime(LocalDateTime.now());
            userAnswerMapper.updateById(existing);
        }

        session.setAnsweredCount(countAnswered(session.getId()));
        sessionMapper.updateById(session);
        cacheSession(session);
        return session;
    }

    public List<UserAnswer> listAnswers(Long sessionId) {
        return userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>()
                        .eq(UserAnswer::getSessionId, sessionId)
                        .eq(UserAnswer::getDeleted, 0)
                        .orderByAsc(UserAnswer::getQuestionId)
        );
    }

    /**
     * 退出测验，不保留该次记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void abandonSession(String sessionKey) {
        QuizSession session = getBySessionKey(sessionKey);
        if (session == null) {
            return;
        }

        // 逻辑删除该会话，避免出现在历史和统计中
        sessionMapper.update(
                null,
                new LambdaUpdateWrapper<QuizSession>()
                        .eq(QuizSession::getId, session.getId())
                        .eq(QuizSession::getDeleted, 0)
                        .set(QuizSession::getStatus, "ABANDONED")
                        .set(QuizSession::getDeleted, 1)
        );

        // 同步删除会话答题记录
        userAnswerMapper.update(
                null,
                new LambdaUpdateWrapper<UserAnswer>()
                        .eq(UserAnswer::getSessionId, session.getId())
                        .eq(UserAnswer::getDeleted, 0)
                        .set(UserAnswer::getDeleted, 1)
        );

        // 删除缓存
        redisTemplate.delete(REDIS_KEY_QUIZ_SESSION + session.getSessionKey());
    }

    private Integer countAnswered(Long sessionId) {
        return Math.toIntExact(userAnswerMapper.selectCount(
                new LambdaQueryWrapper<UserAnswer>()
                        .eq(UserAnswer::getSessionId, sessionId)
                        .eq(UserAnswer::getDeleted, 0)
                        .and(w -> w.isNotNull(UserAnswer::getUserAnswer).ne(UserAnswer::getUserAnswer, ""))
        ));
    }

    private BigDecimal calculateSessionScore(Long sessionId) {
        List<UserAnswer> answers = listAnswers(sessionId);
        return answers.stream()
                .map(a -> a.getScore() == null ? BigDecimal.ZERO : a.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeAnswerValue(Object answer) {
        if (answer == null) {
            return "";
        }
        if (answer instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(answer).trim();
    }

    private Evaluation evaluateAnswer(Question question, Object userAnswer) {
        String type = question.getQuestionType() == null ? "" : question.getQuestionType().toUpperCase(Locale.ROOT);
        String correctAnswer = question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer().trim();
        int fullScore = question.getScore() == null ? 10 : question.getScore();

        if (correctAnswer.isEmpty()) {
            return new Evaluation(false, BigDecimal.ZERO, null);
        }

        if ("MULTIPLE_CHOICE".equals(type)) {
            Set<String> correctSet = Arrays.stream(correctAnswer.split(","))
                    .map(this::normalizeObjectiveToken)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            Set<String> answerSet = Arrays.stream(normalizeAnswerValue(userAnswer).split(","))
                    .map(this::normalizeObjectiveToken)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            boolean ok = !correctSet.isEmpty() && correctSet.equals(answerSet);
            return new Evaluation(ok, ok ? BigDecimal.valueOf(fullScore) : BigDecimal.ZERO, null);
        }

        if ("SINGLE_CHOICE".equals(type)) {
            boolean ok = normalizeObjectiveToken(correctAnswer).equals(normalizeObjectiveToken(normalizeAnswerValue(userAnswer)));
            return new Evaluation(ok, ok ? BigDecimal.valueOf(fullScore) : BigDecimal.ZERO, null);
        }

        return evaluateSubjectiveAnswer(question, normalizeAnswerValue(userAnswer), fullScore);
    }

    private Evaluation evaluateSubjectiveAnswer(Question question, String userAnswer, int fullScore) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return new Evaluation(false, BigDecimal.ZERO, "未作答，建议先覆盖题目中的关键概念，再给出结构化结论。");
        }
        String normalizedUser = userAnswer.trim();
        String normalizedCorrect = question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer().trim();
        if (!normalizedCorrect.isBlank() && normalizedCorrect.equalsIgnoreCase(normalizedUser)) {
            return new Evaluation(true, BigDecimal.valueOf(fullScore), "答案与参考答案高度一致，逻辑完整。");
        }

        Set<String> keyPoints = extractKeyPoints(question);
        if (keyPoints.isEmpty()) {
            return new Evaluation(false, BigDecimal.ZERO, "未提取到可评分要点，建议补充更明确的参考答案。");
        }

        String userLower = normalizedUser.toLowerCase(Locale.ROOT);
        Set<String> matchedPoints = new LinkedHashSet<>();
        Set<String> missingPoints = new LinkedHashSet<>();
        for (String keyPoint : keyPoints) {
            if (userLower.contains(keyPoint.toLowerCase(Locale.ROOT))) {
                matchedPoints.add(keyPoint);
            } else {
                missingPoints.add(keyPoint);
            }
        }
        int matched = matchedPoints.size();

        if (matched <= 0) {
            return new Evaluation(
                    false,
                    BigDecimal.ZERO,
                    "未覆盖参考要点。建议至少说明：" + String.join("、", missingPoints.stream().limit(3).toList()) + "。"
            );
        }

        BigDecimal ratio = BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(keyPoints.size()), 4, RoundingMode.HALF_UP);
        BigDecimal partial = BigDecimal.valueOf(fullScore).multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        if (partial.compareTo(BigDecimal.ZERO) > 0 && partial.compareTo(BigDecimal.valueOf(2)) < 0) {
            partial = BigDecimal.valueOf(2);
        }
        if (partial.compareTo(BigDecimal.valueOf(fullScore)) > 0) {
            partial = BigDecimal.valueOf(fullScore);
        }
        boolean correct = partial.compareTo(BigDecimal.valueOf(fullScore * 0.8)) >= 0;
        String feedback = "命中要点 " + matched + "/" + keyPoints.size() + "，"
                + "得分 " + partial.stripTrailingZeros().toPlainString() + "/" + fullScore + "。";
        if (!missingPoints.isEmpty()) {
            feedback += " 建议补充：" + String.join("、", missingPoints.stream().limit(3).toList()) + "。";
        }
        return new Evaluation(correct, partial, feedback);
    }

    private Set<String> extractKeyPoints(Question question) {
        Set<String> points = new LinkedHashSet<>();
        List<String> sources = new ArrayList<>();
        if (question.getCorrectAnswer() != null) {
            sources.add(question.getCorrectAnswer());
        }
        if (question.getExplanation() != null) {
            sources.add(question.getExplanation());
        }
        for (String source : sources) {
            String normalized = source
                    .replace("\n", " ")
                    .replace("（", "(")
                    .replace("）", ")");
            String[] chunks = normalized.split("[,，。;；:：、|/()]");
            for (String chunk : chunks) {
                String token = chunk.trim().toLowerCase(Locale.ROOT);
                if (token.isBlank()) {
                    continue;
                }
                if (isMeaningfulKeyPoint(token)) {
                    points.add(token);
                }
            }
        }
        return points;
    }

    private boolean isMeaningfulKeyPoint(String token) {
        if (token.length() < 2) {
            return false;
        }
        String clean = token.replaceAll("\\s+", " ").trim();
        if (clean.length() >= 4) {
            return true;
        }
        return clean.matches(".*[\\u4e00-\\u9fa5].*") && clean.length() >= 2;
    }

    private String normalizeObjectiveToken(String raw) {
        if (raw == null) {
            return "";
        }
        String token = raw.trim().toUpperCase(Locale.ROOT);
        if (token.matches("\\d+")) {
            int idx = Integer.parseInt(token);
            if (idx >= 0 && idx <= 25) {
                return String.valueOf((char) ('A' + idx));
            }
        }
        return token;
    }

    private record Evaluation(boolean correct, BigDecimal score, String feedback) {}

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
            QuizSession cached = sessionMapper.selectById(Long.parseLong(value));
            if (cached != null && (cached.getDeleted() == null || cached.getDeleted() == 0)) {
                return cached;
            }
            redisTemplate.delete(key);
        }

        return null;
    }
}
