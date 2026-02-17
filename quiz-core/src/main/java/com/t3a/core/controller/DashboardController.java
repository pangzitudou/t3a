package com.t3a.core.controller;

import com.t3a.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard statistics interface
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics interface")
public class DashboardController {

    @Operation(summary = "Get dashboard statistics")
    @GetMapping
    public Result<DashboardStats> getDashboard() {
        log.info("获取dashboard统计数据");

        try {
            // TODO: Implement actual statistics calculation from database
            // For now, return placeholder data
            DashboardStats stats = new DashboardStats();
            stats.setQuizzesCompleted(0);
            stats.setAverageScore(BigDecimal.ZERO);
            stats.setStudyTime(0);
            stats.setCurrentStreak(0);
            stats.setRecentQuizzes(new ArrayList<>());

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取dashboard数据失败", e);
            return Result.error(e.getMessage());
        }
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
