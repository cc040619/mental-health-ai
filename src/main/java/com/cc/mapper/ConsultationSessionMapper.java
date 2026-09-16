package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminConsultationListResponseDTO;
import com.cc.entity.ConsultationSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 咨询会话表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-01
 */
@Mapper
public interface ConsultationSessionMapper extends BaseMapper<ConsultationSession> {

    /**
     * 按指定ID插入会话（用于流式对话时按原ID重建已删除的会话，
     * 保证前端未刷新页面时当前对话可无缝继续）
     *
     * @return 受影响行数（0表示该ID已被并发请求占用）
     */
    @Insert("INSERT INTO consultation_session (id, user_id, session_title, started_at) " +
            "VALUES (#{id}, #{userId}, #{sessionTitle}, #{startedAt})")
    int insertWithId(ConsultationSession session);

    /**
     * 后台会话分页：单条聚合 SQL（LEFT JOIN 消息表 COUNT + MAX，时长/消息数不落库，禁止 N+1）
     *
     * <p>duration_seconds = 最后消息时间 - started_at，无消息时为 NULL（前端显示「—」）；
     * 筛选条件均在本层 WHERE 完成，走 idx_started / idx_scene 索引。</p>
     */
    @Select("""
            <script>
            SELECT s.id, s.user_id AS userId,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS userNickname,
                   s.session_title AS sessionTitle, s.scene,
                   s.emotion_tag AS emotionTag, s.rating,
                   s.is_high_quality AS isHighQuality, s.is_crisis AS isCrisis,
                   s.started_at AS startedAt,
                   COUNT(m.id) AS msgCount,
                   TIMESTAMPDIFF(SECOND, s.started_at, MAX(m.created_at)) AS durationSeconds
            FROM consultation_session s
            LEFT JOIN consultation_message m ON m.session_id = s.id
            LEFT JOIN user u ON u.id = s.user_id
            <where>
                <if test='keyword != null and keyword != ""'>
                    AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                         OR s.session_title LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test='scene != null and scene != ""'>AND s.scene = #{scene}</if>
                <if test='emotionTag != null and emotionTag != ""'>AND s.emotion_tag = #{emotionTag}</if>
                <if test='ratingMin != null'>AND s.rating &gt;= #{ratingMin}</if>
                <if test='ratingMax != null'>AND s.rating &lt;= #{ratingMax}</if>
                <if test='startDate != null and startDate != ""'>AND s.started_at &gt;= #{startDate}</if>
                <if test='endDate != null and endDate != ""'>AND s.started_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>
                <if test='isCrisis != null'>AND s.is_crisis = #{isCrisis}</if>
            </where>
            GROUP BY s.id
            ORDER BY s.started_at DESC
            </script>
            """)
    IPage<AdminConsultationListResponseDTO> selectAdminPage(Page<AdminConsultationListResponseDTO> page,
                                                            @Param("keyword") String keyword,
                                                            @Param("scene") String scene,
                                                            @Param("emotionTag") String emotionTag,
                                                            @Param("ratingMin") Integer ratingMin,
                                                            @Param("ratingMax") Integer ratingMax,
                                                            @Param("startDate") String startDate,
                                                            @Param("endDate") String endDate,
                                                            @Param("isCrisis") Integer isCrisis);
}
