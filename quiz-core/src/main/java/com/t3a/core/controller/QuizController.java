package com.t3a.core.controller;

import com.t3a.common.domain.Result;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.service.QuizSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
            QuizSession session = sessionService.getBySessionKey(sessionKey);
            if (session == null) {
                return Result.error("会话不存在");
            }
            return Result.success(session);
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
                return Result.error("会话不存在");
            }

            // TODO: Build detailed quiz result with answers
            QuizResult result = new QuizResult();
            result.setSessionKey(session.getSessionKey());
            result.setScore(session.getUserScore() != null ? session.getUserScore().intValue() : 0);
            result.setTotal(100);
            result.setPercentage(session.getUserScore() != null ? session.getUserScore().intValue() : 0);
            result.setTimeTaken(session.getTimeSpent() != null ? session.getTimeSpent() : 0);
            result.setCompletedAt(session.getSubmitTime() != null ? session.getSubmitTime().toString() : null);

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
        private String questionId;
        private Object answer;
    }

    @Data
    public static class QuizResult {
        private String sessionKey;
        private Integer score;
        private Integer total;
        private Integer percentage;
        private Integer timeTaken;
        private String completedAt;
    }
}
