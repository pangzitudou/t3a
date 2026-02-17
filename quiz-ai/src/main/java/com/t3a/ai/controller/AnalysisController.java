package com.t3a.ai.controller;

import com.t3a.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Analysis interface for knowledge gap analysis
 */
@Slf4j
@RestController
@RequestMapping("/ai/analysis")
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "AI知识分析接口")
public class AnalysisController {

    @Operation(summary = "Get AI analysis for a completed quiz")
    @GetMapping("/{sessionKey}")
    public Result<KnowledgeAnalysis> getAnalysis(@PathVariable String sessionKey) {
        log.info("获取AI分析: sessionKey={}", sessionKey);

        try {
            // TODO: Implement actual AI analysis using LLM
            // For now, return placeholder data
            KnowledgeAnalysis analysis = new KnowledgeAnalysis();
            analysis.setSessionKey(sessionKey);
            analysis.setStrengths(new ArrayList<>());
            analysis.setWeaknesses(new ArrayList<>());
            analysis.setOverallFeedback("AI analysis feature coming soon");
            analysis.setStudyPlan(new ArrayList<>());

            return Result.success(analysis);
        } catch (Exception e) {
            log.error("获取AI分析失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "Regenerate AI analysis")
    @PostMapping("/{sessionKey}/regenerate")
    public Result<KnowledgeAnalysis> regenerateAnalysis(@PathVariable String sessionKey) {
        log.info("重新生成AI分析: sessionKey={}", sessionKey);

        try {
            // TODO: Implement actual AI regeneration using LLM
            KnowledgeAnalysis analysis = new KnowledgeAnalysis();
            analysis.setSessionKey(sessionKey);
            analysis.setStrengths(new ArrayList<>());
            analysis.setWeaknesses(new ArrayList<>());
            analysis.setOverallFeedback("Regenerated analysis coming soon");
            analysis.setStudyPlan(new ArrayList<>());

            return Result.success("分析重新生成成功", analysis);
        } catch (Exception e) {
            log.error("重新生成AI分析失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class KnowledgeAnalysis {
        private String sessionKey;
        private List<TopicAnalysis> strengths;
        private List<TopicAnalysis> weaknesses;
        private String overallFeedback;
        private List<String> studyPlan;
    }

    @Data
    public static class TopicAnalysis {
        private String topic;
        private Integer score;
        private List<String> recommendations;
    }
}
