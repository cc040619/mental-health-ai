package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.Consultant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 咨询师 Mapper
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface ConsultantMapper extends BaseMapper<Consultant> {

    /**
     * 管理端状态 Tab 计数：一次 GROUP BY 返回 {status, cnt}，命中 idx_status_created。
     * 无记录的状态不返回行，调用方补 0。
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM consultant GROUP BY status")
    List<Map<String, Object>> countGroupByStatus();
}
