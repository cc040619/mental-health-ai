package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CrisisEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 危机事件记录 Mapper（仅插入元数据，不查询对话内容）
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface CrisisEventMapper extends BaseMapper<CrisisEvent> {
}
