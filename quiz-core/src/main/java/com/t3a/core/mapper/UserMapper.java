package com.t3a.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.t3a.core.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
