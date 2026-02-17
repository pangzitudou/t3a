package com.t3a.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.t3a.core.domain.entity.QuizSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测验会话 Mapper
 */
@Mapper
public interface QuizSessionMapper extends BaseMapper<QuizSession> {
}
