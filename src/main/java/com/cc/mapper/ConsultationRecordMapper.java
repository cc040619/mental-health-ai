package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.ConsultationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 咨询记录 Mapper：单表 CRUD 走 BaseMapper。
 * 一单一记录（uk_appointment）；按 user_id 查询构成历史咨询记录。
 *
 * @author cc
 * @date 2026-09-17
 */
@Mapper
public interface ConsultationRecordMapper extends BaseMapper<ConsultationRecord> {
}
