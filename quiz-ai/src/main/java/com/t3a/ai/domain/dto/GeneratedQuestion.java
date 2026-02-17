package com.t3a.ai.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI生成的题目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestion {

    /**
     * 题目类型
     */
    private String questionType;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 选项（针对选择题）
     */
    private List<String> options;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 解析说明
     */
    private String explanation;

    /**
     * 难度
     */
    private String difficulty;

    /**
     * 知识点标签
     */
    private List<String> tags;

    /**
     * 分值
     */
    private Integer score;
}
