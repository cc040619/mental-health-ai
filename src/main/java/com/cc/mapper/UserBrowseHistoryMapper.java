package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.BrowseHistoryResponseDTO;
import com.cc.dto.response.NameCountDTO;
import com.cc.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 查询某文章近 N 天每日浏览次数（按 created_at 日期分组）
     *
     * @param articleId 文章ID
     * @param startDate 起始时间
     * @return 每日 [{name: 日期字符串, value: 次数}]
     */
    @Select("""
            SELECT DATE_FORMAT(h.created_at, '%Y-%m-%d') AS name, COUNT(*) AS value
            FROM user_browse_history h
            WHERE h.article_id = #{articleId} AND h.created_at >= #{startDate}
            GROUP BY DATE_FORMAT(h.created_at, '%Y-%m-%d')
            ORDER BY name
            """)
    List<NameCountDTO> selectDailyBrowseTrend(@Param("articleId") String articleId,
                                               @Param("startDate") LocalDateTime startDate);

    /**
     * 查询某文章阅读时长统计（平均/累计/总浏览人次）
     *
     * @param articleId 文章ID
     * @return {avgDuration, totalDuration, totalViews}
     */
    @Select("""
            SELECT COALESCE(ROUND(AVG(h.duration_seconds)), 0) AS avgDuration,
                   COALESCE(SUM(h.duration_seconds), 0) AS totalDuration,
                   COUNT(*) AS totalViews
            FROM user_browse_history h
            WHERE h.article_id = #{articleId}
            """)
    Map<String, Object> selectDurationStats(@Param("articleId") String articleId);
}
