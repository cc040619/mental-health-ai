package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
