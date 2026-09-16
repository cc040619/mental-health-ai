package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminReviewResponseDTO;
import com.cc.entity.ConsultantReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 咨询师评价 Mapper
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface ConsultantReviewMapper extends BaseMapper<ConsultantReview> {

    /**
     * 管理端评价分页：JOIN 咨询师 + 预约用户 + 预约日期
     * 支持咨询师/评分筛选
     */
    @Select("""
            <script>
            SELECT r.id, r.appointment_id AS appointmentId, r.consultant_id AS consultantId,
                   c.name AS consultantName, r.user_id AS userId,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS userNickname,
                   r.rating, r.content, r.reply_content AS replyContent, r.reply_at AS replyAt,
                   r.created_at AS createdAt, a.appointment_date AS appointmentDate
            FROM consultant_review r
            INNER JOIN consultant c ON c.id = r.consultant_id
            INNER JOIN user u ON u.id = r.user_id
            INNER JOIN appointment a ON a.id = r.appointment_id
            <where>
                <if test='consultantId != null'>AND r.consultant_id = #{consultantId}</if>
                <if test='rating != null'>AND r.rating = #{rating}</if>
                <if test='keyword != null and keyword != ""'>
                    AND (r.content LIKE CONCAT('%', #{keyword}, '%')
                         OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
                </if>
            </where>
            ORDER BY r.created_at DESC
            </script>
            """)
    IPage<AdminReviewResponseDTO> selectAdminPage(Page<AdminReviewResponseDTO> page,
                                                  @Param("consultantId") Long consultantId,
                                                  @Param("rating") Integer rating,
                                                  @Param("keyword") String keyword);

    /**
     * 全量评分聚合：删除评价后重算 consultant 冗余评分使用。
     * 无评价时返回 null（count=0, avg=null），由 Service 归零。
     */
    @Select("""
            SELECT COUNT(*) AS count, AVG(rating) AS avgRating
            FROM consultant_review
            WHERE consultant_id = #{consultantId}
            """)
    Map<String, Object> selectRatingStats(@Param("consultantId") Long consultantId);
}
