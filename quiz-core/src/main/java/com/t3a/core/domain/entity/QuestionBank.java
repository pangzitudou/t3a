package com.t3a.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库实体
 */
@Data
@TableName("t_question_bank")
public class QuestionBank {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题库名称
     */
    private String name;

    /**
     * 题库描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 是否AI生成
     */
    private Boolean aiGenerated;

    /**
     * 逻辑删除
     */
    private Integer deleted;

    /**
     * 题目数量（非数据库字段，运行时计算）
     */
    @TableField(exist = false)
    private Long questionCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
