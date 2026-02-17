package com.t3a.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.t3a.core.domain.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目 Mapper
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
