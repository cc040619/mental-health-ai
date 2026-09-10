package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AssessmentScale;
import org.apache.ibatis.annotations.Mapper;

/**
 * 心理量表定义 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-08
 */
@Mapper
public interface AssessmentScaleMapper extends BaseMapper<AssessmentScale> {
}
