package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.BrowseHistoryResponseDTO;
import com.cc.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户浏览历史表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-07
 */
@Mapper
public interface UserBrowseHistoryMapper extends BaseMapper<UserBrowseHistory> {

    /**
     * 分页查询当前用户浏览历史，关联文章表返回标题/封面/摘要等信息
     *
     * <p>按最近浏览时间倒序。</p>
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT h.id AS historyId, h.article_id AS articleId, h.created_at AS browseTime,
                   a.title, a.summary, a.cover_image AS coverImage, a.category_id AS categoryId,
                   c.category_name AS categoryName, a.author_id AS authorId,
                   u.nickname AS authorName, a.read_count AS readCount,
                   a.status AS articleStatus, a.published_at AS publishedAt
            FROM user_browse_history h
            INNER JOIN knowledge_article a ON a.id = h.article_id
            LEFT JOIN knowledge_category c ON c.id = a.category_id
            LEFT JOIN user u ON u.id = a.author_id
            WHERE h.user_id = #{userId}
            ORDER BY h.created_at DESC
            </script>
            """)
    IPage<BrowseHistoryResponseDTO> selectHistoryPage(Page<BrowseHistoryResponseDTO> page,
                                                       @Param("userId") Long userId);
}
