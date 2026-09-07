package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识分类表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {
}
