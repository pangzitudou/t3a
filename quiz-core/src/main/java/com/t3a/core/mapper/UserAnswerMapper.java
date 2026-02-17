package com.t3a.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.t3a.core.domain.entity.UserAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户答案 Mapper
 */
@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {
}
