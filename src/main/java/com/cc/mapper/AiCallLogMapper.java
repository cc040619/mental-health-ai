package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * AI 调用日志 Mapper（统计聚合走 idx_created 索引）
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {

    /**
     * 按日聚合：调用次数/成功数/平均耗时/token 总量（token 有值才聚合，无值列为 NULL）
     */
    @Select("""
            SELECT DATE(created_at) AS date,
                   COUNT(*) AS count,
                   SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS successCount,
                   AVG(duration_ms) AS avgDurationMs,
                   SUM(input_tokens) AS inputTokens,
                   SUM(output_tokens) AS outputTokens
            FROM ai_call_log
            WHERE created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY date
            """)
    List<Map<String, Object>> selectDailyStats(@Param("since") String since);

    /**
     * 用途分布：各 purpose 调用次数
     */
    @Select("""
            SELECT purpose, COUNT(*) AS count
            FROM ai_call_log
            WHERE created_at >= #{since}
            GROUP BY purpose
            ORDER BY count DESC
            """)
    List<Map<String, Object>> selectPurposeStats(@Param("since") String since);
}
