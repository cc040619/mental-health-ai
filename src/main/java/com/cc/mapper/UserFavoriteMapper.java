package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.FavoriteResponseDTO;
import com.cc.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户收藏表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-07
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 分页查询当前用户收藏的文章列表，关联文章表返回标题/封面/摘要等信息
     *
     * <p>仅返回已发布（status=1）或已下线（status=2）的文章，草稿文章不出现在用户收藏列表中。</p>
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT f.id AS favoriteId, f.article_id AS articleId, f.created_at AS favoriteTime,
                   a.title, a.summary, a.cover_image AS coverImage, a.category_id AS categoryId,
                   c.category_name AS categoryName, a.author_id AS authorId,
                   u.nickname AS authorName, a.read_count AS readCount,
                   a.status AS articleStatus, a.published_at AS publishedAt
            FROM user_favorite f
            INNER JOIN knowledge_article a ON a.id = f.article_id
            LEFT JOIN knowledge_category c ON c.id = a.category_id
            LEFT JOIN user u ON u.id = a.author_id
            WHERE f.user_id = #{userId}
            ORDER BY f.created_at DESC
            </script>
            """)
    IPage<FavoriteResponseDTO> selectFavoritePage(Page<FavoriteResponseDTO> page,
                                                   @Param("userId") Long userId);
}
