package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
}
