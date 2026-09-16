package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AiBlockRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 回复拦截记录 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface AiBlockRecordMapper extends BaseMapper<AiBlockRecord> {
}
