package com.t3a.ai.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 题目生成请求
 */
@Data
public class GenerateQuestionRequest {

    /**
     * 题库ID（可选，如果不指定则创建新题库）
     */
    private Long bankId;

    /**
     * 题库名称（创建新题库时必填）
     */
    private String bankName;

    /**
     * 题目数量
     */
    @NotNull(message = "题目数量不能为空")
    @Min(value = 5, message = "题目数量不能少于5")
    @Max(value = 50, message = "题目数量不能超过50")
    private Integer count;

    /**
     * 难度级别：EASY, MEDIUM, HARD, MIXED
     */
    @NotBlank(message = "难度级别不能为空")
    private String difficulty;

    /**
     * 题型分布（JSON格式）
     * 例如: {"SINGLE_CHOICE": 40, "MULTIPLE_CHOICE": 30, "SHORT_ANSWER": 30}
     */
    private String typeDistribution;

    /**
     * 知识领域
     */
    private String category;
}
