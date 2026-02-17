package com.t3a.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.t3a.core.domain.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库 Mapper
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {
}
