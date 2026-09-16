package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminRecordResponseDTO;
import com.cc.dto.response.AlertResponseDTO;
import com.cc.dto.response.AssessmentRecordResponseDTO;
import com.cc.dto.response.AssessmentTrendResponseDTO;
import com.cc.entity.AssessmentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 测评作答记录 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-08
 */
@Mapper
public interface AssessmentRecordMapper extends BaseMapper<AssessmentRecord> {

    /**
     * 分页查询用户已完成测评记录列表，关联量表返回名称与类别
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param scaleId 量表ID（可空）
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT r.id, r.user_id, r.scale_id, r.total_score, r.level, r.risk_flag,
                   r.completed_at, r.created_at, s.code AS scale_code, s.name AS scale_name,
                   s.category AS scale_category
            FROM assessment_record r
            LEFT JOIN assessment_scale s ON s.id = r.scale_id
            <where>
                r.user_id = #{userId} AND r.status = 'COMPLETED'
                <if test="scaleId != null">AND r.scale_id = #{scaleId}</if>
            </where>
            ORDER BY r.completed_at DESC
            </script>
            """)
    IPage<AssessmentRecordResponseDTO> selectCompletedPage(Page<AssessmentRecordResponseDTO> page,
                                                           @Param("userId") Long userId,
                                                           @Param("scaleId") Long scaleId);

    /**
     * 查询同量表历史趋势（已完成记录，按时间升序）
     *
     * @param userId  用户ID
     * @param scaleId 量表ID
     * @return 趋势列表
     */
    @Select("""
            SELECT r.id, r.total_score, r.level, r.completed_at
            FROM assessment_record r
            WHERE r.user_id = #{userId} AND r.scale_id = #{scaleId} AND r.status = 'COMPLETED'
            ORDER BY r.completed_at ASC
            """)
    List<AssessmentTrendResponseDTO> selectTrend(@Param("userId") Long userId,
                                                 @Param("scaleId") Long scaleId);

    /**
     * 管理端测评记录分页：关联量表名 + 用户昵称，多条件筛选
     */
    @Select("""
            <script>
            SELECT r.id, r.user_id, u.nickname AS user_nickname, r.scale_id, s.name AS scale_name,
                   r.total_score, r.level, r.risk_flag, r.completed_at
            FROM assessment_record r
            LEFT JOIN assessment_scale s ON s.id = r.scale_id
            LEFT JOIN user u ON u.id = r.user_id
            <where>
                r.status = 'COMPLETED'
                <if test="scaleId != null">AND r.scale_id = #{scaleId}</if>
                <if test="keyword != null and keyword != ''">
                    AND u.nickname LIKE CONCAT('%', #{keyword}, '%')
                </if>
                <if test="level != null and level != ''">AND r.level = #{level}</if>
                <if test="startDate != null and startDate != ''">
                    AND r.completed_at <![CDATA[>=]]> #{startDate}
                </if>
                <if test="endDate != null and endDate != ''">
                    AND r.completed_at <![CDATA[<=]]> #{endDate}
                </if>
            </where>
            ORDER BY r.completed_at DESC
            </script>
            """)
    IPage<AdminRecordResponseDTO> selectAdminPage(Page<AdminRecordResponseDTO> page,
                                                  @Param("scaleId") Long scaleId,
                                                  @Param("keyword") String keyword,
                                                  @Param("level") String level,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);

    /**
     * 高危预警分页：level IN (MODERATE,SEVERE) OR risk_flag=1
     */
    @Select("""
            <script>
            SELECT r.id, r.user_id, u.nickname AS user_nickname, r.scale_id, s.name AS scale_name,
                   r.total_score, r.level, r.risk_flag, r.intervention_status,
                   r.intervention_note, r.intervened_at, r.completed_at
            FROM assessment_record r
            LEFT JOIN assessment_scale s ON s.id = r.scale_id
            LEFT JOIN user u ON u.id = r.user_id
            <where>
                r.status = 'COMPLETED'
                AND (r.level IN ('MODERATE', 'SEVERE') OR r.risk_flag = 1)
                <if test="level != null and level != ''">AND r.level = #{level}</if>
                <if test="status != null">AND r.intervention_status = #{status}</if>
            </where>
            ORDER BY r.completed_at DESC
            </script>
            """)
    IPage<AlertResponseDTO> selectAlertsPage(Page<AlertResponseDTO> page,
                                             @Param("level") String level,
                                             @Param("status") Integer status);
}
