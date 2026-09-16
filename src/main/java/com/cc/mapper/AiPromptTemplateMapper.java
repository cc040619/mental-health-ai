package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AiPromptTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 提示词模板 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {
}
