package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminAnnouncementListResponseDTO;
import com.cc.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统公告 Mapper
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 后台公告分页：LEFT JOIN 阅读表一次聚合 read_count（禁止 N+1），
     * 类型/状态/关键字筛选在本层 WHERE 完成
     */
    @Select("""
            <script>
            SELECT a.id, a.title, a.content, a.type, a.status,
                   a.target_type AS targetType, a.target_user_ids AS targetUserIdsJson,
                   a.publish_at AS publishAt, a.published_at AS publishedAt, a.created_at AS createdAt,
                   COUNT(r.id) AS readCount
            FROM announcement a
            LEFT JOIN announcement_read r ON r.announcement_id = a.id
            <where>
                <if test='type != null and type != ""'>AND a.type = #{type}</if>
                <if test='status != null'>AND a.status = #{status}</if>
                <if test='keyword != null and keyword != ""'>AND a.title LIKE CONCAT('%', #{keyword}, '%')</if>
            </where>
            GROUP BY a.id
            ORDER BY a.created_at DESC
            </script>
            """)
    IPage<AdminAnnouncementListResponseDTO> selectAdminPage(Page<AdminAnnouncementListResponseDTO> page,
                                                            @Param("type") String type,
                                                            @Param("status") Integer status,
                                                            @Param("keyword") String keyword);
}
