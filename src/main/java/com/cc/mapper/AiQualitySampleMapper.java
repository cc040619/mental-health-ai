package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AiQualitySample;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 对话质量抽检 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface AiQualitySampleMapper extends BaseMapper<AiQualitySample> {

    /**
     * 随机抽 N 个有消息且未抽过的会话（去重：JOIN 排除已抽）。
     * ponytail: ORDER BY RAND() 全表扫描，毕设会话量小可接受；量大改按时间窗口分段抽样
     */
    @Select("""
            SELECT s.id FROM consultation_session s
            WHERE EXISTS (SELECT 1 FROM consultation_message m WHERE m.session_id = s.id)
              AND s.id NOT IN (SELECT session_id FROM ai_quality_sample)
            ORDER BY RAND()
            LIMIT #{count}
            """)
    List<Long> selectRandomSessionIds(@Param("count") int count);

    /**
     * 抽检列表：JOIN 会话与用户取昵称/时间/消息数（子查询计数，量小可接受）
     */
    @Select("""
            <script>
            SELECT q.id, q.session_id AS sessionId, q.status, q.score, q.note,
                   q.created_at AS createdAt, q.annotated_at AS annotatedAt,
                   u.nickname, s.session_title AS sessionTitle, s.scene, s.started_at AS startedAt,
                   (SELECT COUNT(*) FROM consultation_message m WHERE m.session_id = s.id) AS messageCount
            FROM ai_quality_sample q
            LEFT JOIN consultation_session s ON s.id = q.session_id
            LEFT JOIN user u ON u.id = s.user_id
            <if test='status != null'>WHERE q.status = #{status}</if>
            ORDER BY q.created_at DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<Map<String, Object>> selectSamplePage(@Param("status") Integer status,
                                               @Param("offset") long offset,
                                               @Param("size") long size);

    /**
     * 抽检总数（与 selectSamplePage 同条件）
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM ai_quality_sample q
            <if test='status != null'>WHERE q.status = #{status}</if>
            </script>
            """)
    long countByStatus(@Param("status") Integer status);

    /**
     * 批量写入抽检样本（一条 INSERT 多 VALUES）
     */
    @Insert("""
            <script>
            INSERT INTO ai_quality_sample (session_id, status, created_at)
            VALUES
            <foreach collection='sessionIds' item='sid' separator=','>
            (#{sid}, 0, NOW())
            </foreach>
            </script>
            """)
    int insertBatch(@Param("sessionIds") List<Long> sessionIds);
}
