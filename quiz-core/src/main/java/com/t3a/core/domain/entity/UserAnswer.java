package com.t3a.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户答题记录
 */
@Data
@TableName("t_user_answer")
public class UserAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;
    private BigDecimal score;
    private String aiFeedback;
    private LocalDateTime answerTime;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
