package com.t3a.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测验会话实体
 */
@Data
@TableName("t_quiz_session")
public class QuizSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题库ID
     */
    private Long bankId;

    /**
     * 会话标识
     */
    private String sessionKey;

    /**
     * 总题数
     */
    private Integer totalQuestions;

    /**
     * 已答题数
     */
    private Integer answeredCount;

    /**
     * 总分
     */
    private Integer totalScore;

    /**
     * 用户得分
     */
    private BigDecimal userScore;

    /**
     * 状态: IN_PROGRESS, COMPLETED, ABANDONED
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 耗时（秒）
     */
    private Integer timeSpent;

    /**
     * 逻辑删除
     */
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
