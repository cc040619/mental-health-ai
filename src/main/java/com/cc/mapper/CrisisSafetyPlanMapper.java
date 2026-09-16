package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CrisisSafetyPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人安全计划 Mapper（单表 CRUD 走 BaseMapper，user_id 唯一键保证一人一行）
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface CrisisSafetyPlanMapper extends BaseMapper<CrisisSafetyPlan> {
}
