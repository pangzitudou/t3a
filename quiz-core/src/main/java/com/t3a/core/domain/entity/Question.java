package com.t3a.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体
 */
@Data
@TableName("t_question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题库ID
     */
    private Long bankId;

    /**
     * 题目类型: SINGLE_CHOICE, MULTIPLE_CHOICE, SHORT_ANSWER, CODE
     */
    private String questionType;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 选项（JSON格式）
     */
    private String options;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 解析说明
     */
    private String explanation;

    /**
     * 难度: EASY, MEDIUM, HARD
     */
    private String difficulty;

    /**
     * 知识点标签（逗号分隔）
     */
    private String tags;

    /**
     * 分值
     */
    private Integer score;

    /**
     * 是否由AI生成
     */
    private Boolean aiGenerated;

    /**
     * 逻辑删除
     */
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
