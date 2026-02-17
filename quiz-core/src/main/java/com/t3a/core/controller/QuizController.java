package com.t3a.core.controller;

import com.t3a.common.domain.Result;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.UserAnswer;
import com.t3a.core.service.QuestionService;
import com.t3a.core.service.QuizSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 测验管理接口
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Quiz Management", description = "测验管理接口")
public class QuizController {

    private final QuizSessionService sessionService;
    private final QuestionService questionService;

    @Operation(summary = "开始测验")
    @PostMapping("/start")
    public Result<QuizSession> startQuiz(@RequestBody StartQuizRequest request) {
        log.info("开始测验: userId={}, bankId={}, count={}",
                request.getUserId(), request.getBankId(), request.getQuestionCount());

        try {
            QuizSession session = sessionService.createSession(
                    request.getUserId(),
                    request.getBankId(),
                    request.getQuestionCount()
            );
            return Result.success("测验已开始", session);
        } catch (IllegalArgumentException e) {
            log.warn("开始测验参数/业务校验失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("开始测验失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "提交测验")
    @PostMapping("/submit")
    public Result<QuizSession> submitQuiz(@RequestBody SubmitQuizRequest request) {
        log.info("提交测验: sessionKey={}", request.getSessionKey());

        try {
            QuizSession session = sessionService.submitSession(
                    request.getSessionKey(),
                    request.getUserScore()
            );
            return Result.success("测验提交成功", session);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("提交测验业务校验失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("提交测验失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/session/{sessionKey}")
    public Result<QuizSession> getSession(@PathVariable String sessionKey) {
        QuizSession session = sessionService.getBySessionKey(sessionKey);
        if (session == null) {
            return Result.error("会话不存在");
        }
        return Result.success(session);
    }

    @Operation(summary = "退出测验（不保留记录）")
    @PostMapping("/session/{sessionKey}/abandon")
    public Result<Void> abandonQuiz(@PathVariable String sessionKey) {
        log.info("退出测验: sessionKey={}", sessionKey);
        try {
            sessionService.abandonSession(sessionKey);
            return Result.success("已退出测验", null);
        } catch (Exception e) {
            log.error("退出测验失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取当前题目")
    @GetMapping("/session/{sessionKey}/current")
    public Result<?> getCurrentQuestion(@PathVariable String sessionKey) {
        QuizSession session = sessionService.getBySessionKey(sessionKey);
        if (session == null) {
            return Result.error("会话不存在");
        }
        return Result.success(session);
    }

    @Operation(summary = "提交答案")
    @PostMapping("/session/{sessionKey}/answer")
    public Result<QuizSession> submitAnswer(
            @PathVariable String sessionKey,
            @RequestBody AnswerRequest request) {
        log.info("提交答案: sessionKey={}, questionId={}", sessionKey, request.getQuestionId());
        try {
            if (request.getQuestionId() == null) {
                return Result.error(400, "questionId不能为空");
            }
            QuizSession session = sessionService.saveAnswer(sessionKey, request.getQuestionId(), request.getAnswer());
            return Result.success(session);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("提交答案业务校验失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("提交答案失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取用户的所有测验记录")
    @GetMapping("/history/{userId}")
    public Result<List<QuizSession>> getUserHistory(@PathVariable Long userId) {
        List<QuizSession> sessions = sessionService.listUserSessions(userId);
        return Result.success(sessions);
    }

    @Operation(summary = "获取测验结果")
    @GetMapping("/result/{sessionKey}")
    public Result<QuizResult> getQuizResult(@PathVariable String sessionKey) {
        log.info("获取测验结果: sessionKey={}", sessionKey);
        try {
            QuizSession session = sessionService.getBySessionKey(sessionKey);
            if (session == null) {
                return Result.error(404, "会话不存在");
            }

            List<Question> questions = questionService.listByBankId(session.getBankId());
            Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
            List<UserAnswer> userAnswers = sessionService.listAnswers(session.getId());

            QuizResult result = new QuizResult();
            result.setSessionKey(session.getSessionKey());
            result.setScore(session.getUserScore() != null ? session.getUserScore().intValue() : 0);
            result.setTotal(session.getTotalScore() == null ? 100 : session.getTotalScore());
            result.setPercentage(result.getTotal() <= 0 ? 0 : Math.round((result.getScore() * 100f) / result.getTotal()));
            result.setTimeTaken(session.getTimeSpent() != null ? session.getTimeSpent() : 0);
            result.setTotalQuestions(session.getTotalQuestions() == null ? 0 : session.getTotalQuestions());
            result.setCompletedAt(session.getSubmitTime() != null ? session.getSubmitTime().toString() : null);
            result.setAnswers(new ArrayList<>());

            for (UserAnswer answer : userAnswers) {
                Question question = questionMap.get(answer.getQuestionId());
                if (question == null) {
                    continue;
                }
                AnswerResult detail = new AnswerResult();
                detail.setQuestionId(String.valueOf(question.getId()));
                detail.setQuestion(question.getContent());
                detail.setQuestionType(question.getQuestionType());
                detail.setOptions(parseOptions(question.getOptions()));
                String referenceAnswer = question.getCorrectAnswer();
                if ((referenceAnswer == null || referenceAnswer.isBlank())
                        && ("SHORT_ANSWER".equalsIgnoreCase(question.getQuestionType())
                        || "CODE".equalsIgnoreCase(question.getQuestionType()))) {
                    referenceAnswer = "请参考解析";
                }
                detail.setCorrectAnswer(referenceAnswer);
                detail.setUserAnswer(answer.getUserAnswer());
                detail.setIsCorrect(answer.getIsCorrect() != null && answer.getIsCorrect() == 1);
                detail.setScore(answer.getScore() == null ? 0 : answer.getScore().intValue());
                detail.setExplanation(question.getExplanation());
                String feedback = answer.getAiFeedback();
                if ((feedback == null || feedback.isBlank())
                        && ("SHORT_ANSWER".equalsIgnoreCase(question.getQuestionType())
                        || "CODE".equalsIgnoreCase(question.getQuestionType()))) {
                    feedback = "该题已按要点评分，建议结合参考答案与解析补充遗漏要点。";
                }
                detail.setAiFeedback(feedback);
                result.getAnswers().add(detail);
            }
            result.setCorrectAnswers((int) result.getAnswers().stream().filter(AnswerResult::getIsCorrect).count());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取测验结果失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class StartQuizRequest {
        private Long userId;
        private Long bankId;
        private Integer questionCount;
    }

    @Data
    public static class SubmitQuizRequest {
        private String sessionKey;
        private BigDecimal userScore;
    }

    @Data
    public static class AnswerRequest {
        private Long questionId;
        private Object answer;
    }

    @Data
    public static class QuizResult {
        private String sessionKey;
        private Integer score;
        private Integer total;
        private Integer percentage;
        private Integer timeTaken;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private String completedAt;
        private List<AnswerResult> answers;
    }

    @Data
    public static class AnswerResult {
        private String questionId;
        private String question;
        private String questionType;
        private List<String> options;
        private String correctAnswer;
        private String userAnswer;
        private Boolean isCorrect;
        private Integer score;
        private String explanation;
        private String aiFeedback;
    }

    private List<String> parseOptions(String rawOptions) {
        if (rawOptions == null || rawOptions.isBlank()) {
            return new ArrayList<>();
        }
        String normalized = rawOptions.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(normalized.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
