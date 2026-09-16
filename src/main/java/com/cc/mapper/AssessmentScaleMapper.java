package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminScaleListResponseDTO;
import com.cc.entity.AssessmentScale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 心理量表定义 Mapper
 *
 * @author cc
 * @date 2026-09-08
 */
@Mapper
public interface AssessmentScaleMapper extends BaseMapper<AssessmentScale> {

    /**
     * 管理端量表列表：LEFT JOIN 聚合测评次数（status=COMPLETED），禁止 N+1
     */
    @Select("""
            <script>
            SELECT s.id, s.code, s.name, s.category, s.question_count, s.estimated_minutes,
                   s.enabled, s.created_at,
                   COUNT(r.id) AS assessment_count
            FROM assessment_scale s
            LEFT JOIN assessment_record r ON r.scale_id = s.id AND r.status = 'COMPLETED'
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (s.name LIKE CONCAT('%', #{keyword}, '%')
                         OR s.code LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="category != null and category != ''">
                    AND s.category = #{category}
                </if>
                <if test="enabled != null">
                    AND s.enabled = #{enabled}
                </if>
            </where>
            GROUP BY s.id
            ORDER BY s.created_at DESC
            </script>
            """)
    IPage<AdminScaleListResponseDTO> selectAdminPage(Page<AdminScaleListResponseDTO> page,
                                                     @Param("keyword") String keyword,
                                                     @Param("category") String category,
                                                     @Param("enabled") Integer enabled);
}
