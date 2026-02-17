package com.t3a.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.t3a.common.domain.Result;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.User;
import com.t3a.core.mapper.QuizSessionMapper;
import com.t3a.core.service.QuestionBankService;
import com.t3a.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dashboard statistics interface
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics interface")
public class DashboardController {
    private final UserService userService;
    private final QuizSessionMapper quizSessionMapper;
    private final QuestionBankService questionBankService;

    @Operation(summary = "Get dashboard statistics")
    @GetMapping
    public Result<DashboardStats> getDashboard() {
        log.info("获取dashboard统计数据");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication == null ? null : authentication.getName();
            if (username == null || username.isBlank()) {
                return Result.success(emptyStats());
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                return Result.success(emptyStats());
            }

            List<QuizSession> sessions = quizSessionMapper.selectList(
                    new LambdaQueryWrapper<QuizSession>()
                            .eq(QuizSession::getUserId, user.getId())
                            .eq(QuizSession::getDeleted, 0)
                            .eq(QuizSession::getStatus, "COMPLETED")
                            .orderByDesc(QuizSession::getSubmitTime)
            );

            DashboardStats stats = buildStats(sessions);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取dashboard数据失败", e);
            return Result.error("获取dashboard数据失败");
        }
    }

    private DashboardStats buildStats(List<QuizSession> sessions) {
        DashboardStats stats = new DashboardStats();
        if (sessions == null || sessions.isEmpty()) {
            return emptyStats();
        }

        stats.setQuizzesCompleted(sessions.size());
        BigDecimal avg = sessions.stream()
                .map(s -> {
                    if (s.getTotalScore() == null || s.getTotalScore() <= 0 || s.getUserScore() == null) {
                        return BigDecimal.ZERO;
                    }
                    return s.getUserScore()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(s.getTotalScore()), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sessions.size()), 2, RoundingMode.HALF_UP);
        stats.setAverageScore(avg);

        int totalSeconds = sessions.stream()
                .map(QuizSession::getTimeSpent)
                .filter(v -> v != null && v > 0)
                .mapToInt(Integer::intValue)
                .sum();
        stats.setStudyTime(totalSeconds / 60);

        stats.setCurrentStreak(calculateCurrentStreak(sessions));

        Map<Long, QuestionBank> bankMap = sessions.stream()
                .map(QuizSession::getBankId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(questionBankService::getById)
                .filter(bank -> bank != null && bank.getId() != null)
                .collect(Collectors.toMap(
                        QuestionBank::getId,
                        bank -> bank,
                        (left, right) -> left
                ));

        List<RecentQuiz> recent = sessions.stream()
                .sorted(Comparator.comparing(QuizSession::getSubmitTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(s -> {
                    RecentQuiz quiz = new RecentQuiz();
                    quiz.setSessionKey(s.getSessionKey());
                    QuestionBank bank = bankMap.get(s.getBankId());
                    quiz.setBankName(bank == null ? "Unknown Bank" : bank.getName());
                    quiz.setScore(s.getUserScore() == null ? BigDecimal.ZERO : s.getUserScore());
                    quiz.setTotalScore(s.getTotalScore() == null ? BigDecimal.ZERO : BigDecimal.valueOf(s.getTotalScore()));
                    quiz.setCompletedAt(s.getSubmitTime() == null ? null : s.getSubmitTime().toString());
                    return quiz;
                })
                .collect(Collectors.toList());
        stats.setRecentQuizzes(recent);

        return stats;
    }

    private int calculateCurrentStreak(List<QuizSession> sessions) {
        Set<LocalDate> days = sessions.stream()
                .map(QuizSession::getSubmitTime)
                .filter(v -> v != null)
                .map(v -> v.toLocalDate())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (days.isEmpty()) {
            return 0;
        }
        int streak = 0;
        LocalDate current = LocalDate.now();
        while (days.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }
        return streak;
    }

    private DashboardStats emptyStats() {
        DashboardStats stats = new DashboardStats();
        stats.setQuizzesCompleted(0);
        stats.setAverageScore(BigDecimal.ZERO);
        stats.setStudyTime(0);
        stats.setCurrentStreak(0);
        stats.setRecentQuizzes(new ArrayList<>());
        return stats;
    }

    @Data
    public static class DashboardStats {
        private Integer quizzesCompleted;
        private BigDecimal averageScore;
        private Integer studyTime; // in minutes
        private Integer currentStreak; // consecutive days
        private List<RecentQuiz> recentQuizzes;
    }

    @Data
    public static class RecentQuiz {
        private String sessionKey;
        private String bankName;
        private BigDecimal score;
        private BigDecimal totalScore;
        private String completedAt;
    }
}
