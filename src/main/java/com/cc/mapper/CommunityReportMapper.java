package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.ReportResponseDTO;
import com.cc.entity.CommunityReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 社区举报 Mapper
 *
 * @author cc
 * @date 2026-09-10
 */
@Mapper
public interface CommunityReportMapper extends BaseMapper<CommunityReport> {

    /**
     * 后台举报分页：举报人昵称 + 举报对象快照（帖子标题/评论内容）
     */
    @Select("""
            <script>
            SELECT r.id, r.target_type AS targetType, r.target_id AS targetId,
                   r.reporter_id AS reporterId, r.reason, r.status,
                   r.created_at AS createdAt, r.handled_at AS handledAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS reporterName,
                   p.title AS postTitle,
                   LEFT(c.content, 100) AS commentContent
            FROM community_report r
            LEFT JOIN user u ON u.id = r.reporter_id
            LEFT JOIN community_post p ON r.target_type = 1 AND p.id = r.target_id
            LEFT JOIN community_comment c ON r.target_type = 2 AND c.id = r.target_id
            <where>
                <if test='status != null'>
                    AND r.status = #{status}
                </if>
            </where>
            ORDER BY r.created_at DESC
            </script>
            """)
    IPage<ReportResponseDTO> selectReportPage(Page<ReportResponseDTO> page, @Param("status") Integer status);
}
