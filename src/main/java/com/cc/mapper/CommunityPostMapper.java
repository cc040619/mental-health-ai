package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.PostResponseDTO;
import com.cc.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区帖子 Mapper
 *
 * <p>列表/搜索/收藏分页用 JOIN + 动态 SQL；匿名屏蔽在 SQL 层完成
 * （showReal=1 表示访问者为作者本人或管理员，可见真实昵称头像）；
 * isLiked/isFavorited 用 EXISTS 子查询批量带出，禁止 N+1。
 * 列表仅取正文前 300 字符原始片段（summary_raw），纯文本摘要由 Service 层清洗。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {

    /** 信息流/搜索/收藏列表公共列（匿名 + 互动状态子查询） */
    String LIST_COLUMNS = """
            p.id, p.board, p.title, LEFT(p.content, 300) AS summary_raw,
            p.images, p.author_id AS authorId, p.is_anonymous AS isAnonymous,
            p.is_top AS isTop, p.is_featured AS isFeatured,
            p.like_count AS likeCount, p.comment_count AS commentCount,
            p.favorite_count AS favoriteCount, p.view_count AS viewCount,
            p.is_crisis AS isCrisis, p.status, p.created_at AS createdAt,
            CASE WHEN p.is_anonymous = 1 AND #{showReal} = 0 THEN '匿名用户'
                 ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS authorName,
            CASE WHEN p.is_anonymous = 1 AND #{showReal} = 0 THEN NULL ELSE u.avatar END AS avatar,
            CASE WHEN p.author_id = #{userId} THEN 1 ELSE 0 END AS isMine,
            CASE WHEN EXISTS (SELECT 1 FROM community_like l WHERE l.post_id = p.id AND l.user_id = #{userId})
                 THEN 1 ELSE 0 END AS isLiked,
            CASE WHEN EXISTS (SELECT 1 FROM community_favorite f WHERE f.post_id = p.id AND f.user_id = #{userId})
                 THEN 1 ELSE 0 END AS isFavorited
            """;

    /**
     * 信息流分页（最新/推荐共用），仅正常帖 status=0；
     * 置顶 > 加精 > 创建时间倒序（置顶不设时长，扩展路径见实体注释）
     */
    @Select("<script>" +
            "SELECT " + LIST_COLUMNS +
            " FROM community_post p" +
            " LEFT JOIN user u ON u.id = p.author_id" +
            " WHERE p.status = 0" +
            " <if test='board != null and board != \"\"'> AND p.board = #{board} </if>" +
            " ORDER BY p.is_top DESC, p.is_featured DESC, p.created_at DESC" +
            "</script>")
    @Results(id = "postListMap", value = {
            @Result(column = "images", property = "images", typeHandler = JacksonTypeHandler.class),
            @Result(column = "summary_raw", property = "summary")
    })
    IPage<PostResponseDTO> selectPostPage(Page<PostResponseDTO> page,
                                          @Param("board") String board,
                                          @Param("userId") Long userId,
                                          @Param("showReal") int showReal);

    /**
     * 管理端帖子分页：板块/标题关键字/状态筛选（匿名帖作者屏蔽为「匿名用户」，真实身份仅举报处理可见）
     * 走 idx_status_created / idx_board_created 索引
     */
    @Select("""
            <script>
            SELECT p.id, p.board, p.title, LEFT(p.content, 300) AS summary_raw,
                   p.images, p.author_id AS authorId, p.is_anonymous AS isAnonymous,
                   p.is_top AS isTop, p.is_featured AS isFeatured, p.reject_reason AS rejectReason,
                   p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.favorite_count AS favoriteCount, p.view_count AS viewCount,
                   p.is_crisis AS isCrisis, p.status, p.created_at AS createdAt,
                   CASE WHEN p.is_anonymous = 1 THEN '匿名用户'
                        ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS authorName
            FROM community_post p
            LEFT JOIN user u ON u.id = p.author_id
            <where>
                <if test='board != null and board != ""'>AND p.board = #{board}</if>
                <if test='status != null'>AND p.status = #{status}</if>
                <if test='keyword != null and keyword != ""'>AND p.title LIKE CONCAT('%', #{keyword}, '%')</if>
            </where>
            ORDER BY p.created_at DESC
            </script>
            """)
    @Results(id = "adminPostListMap", value = {
            @Result(column = "images", property = "images", typeHandler = JacksonTypeHandler.class),
            @Result(column = "summary_raw", property = "summary")
    })
    IPage<PostResponseDTO> selectAdminPostPage(Page<PostResponseDTO> page,
                                               @Param("board") String board,
                                               @Param("keyword") String keyword,
                                               @Param("status") Integer status);

    /**
     * 热门榜候选：score = 点赞*2 + 评论*3 + 收藏*2 + 浏览*0.5，带时间衰减
     * <p>只统计近 30 天帖子，由 Service 层缓存为热门榜；热门榜不按访问者区分，匿名统一屏蔽</p>
     */
    @Select("""
            SELECT p.id, p.board, p.title, p.images, p.author_id AS authorId,
                   p.is_anonymous AS isAnonymous,
                   p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.favorite_count AS favoriteCount, p.view_count AS viewCount,
                   p.is_crisis AS isCrisis, p.status, p.created_at AS createdAt,
                   CASE WHEN p.is_anonymous = 1 THEN '匿名用户'
                        ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS authorName,
                   CASE WHEN p.is_anonymous = 1 THEN NULL ELSE u.avatar END AS avatar
            FROM community_post p
            LEFT JOIN user u ON u.id = p.author_id
            WHERE p.status = 0
              AND p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            ORDER BY (p.like_count * 2 + p.comment_count * 3 + p.favorite_count * 2 + p.view_count * 0.5)
                     / (1 + DATEDIFF(NOW(), p.created_at) * 0.3) DESC
            LIMIT #{limit}
            """)
    @Results(id = "hotListMap", value = {
            @Result(column = "images", property = "images", typeHandler = JacksonTypeHandler.class),
            @Result(column = "summary_raw", property = "summary")
    })
    List<PostResponseDTO> selectHotPosts(@Param("limit") int limit);

    /**
     * 关键词搜索：标题前缀 LIKE（keyword%，可走索引）+ 正文 LIKE，仅正常帖
     * <p>ponytail: MySQL LIKE 覆盖毕设规模搜索；数据量上来后再上搜索引擎</p>
     */
    @Select("<script>" +
            "SELECT " + LIST_COLUMNS +
            " FROM community_post p" +
            " LEFT JOIN user u ON u.id = p.author_id" +
            " WHERE p.status = 0" +
            " AND (p.title LIKE CONCAT(#{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%'))" +
            " ORDER BY p.created_at DESC" +
            "</script>")
    @Results(id = "searchListMap", value = {
            @Result(column = "images", property = "images", typeHandler = JacksonTypeHandler.class),
            @Result(column = "summary_raw", property = "summary")
    })
    IPage<PostResponseDTO> searchPosts(Page<PostResponseDTO> page,
                                       @Param("keyword") String keyword,
                                       @Param("userId") Long userId,
                                       @Param("showReal") int showReal);

    /**
     * 我的收藏帖子分页（本人看自己收藏，作者真实身份可见）
     */
    @Select("""
            SELECT p.id, p.board, p.title, LEFT(p.content, 300) AS summary_raw,
                   p.images, p.author_id AS authorId, p.is_anonymous AS isAnonymous,
                   p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.favorite_count AS favoriteCount, p.view_count AS viewCount,
                   p.is_crisis AS isCrisis, p.status, p.created_at AS createdAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS authorName,
                   u.avatar,
                   CASE WHEN p.author_id = #{userId} THEN 1 ELSE 0 END AS isMine,
                   1 AS isFavorited,
                   CASE WHEN EXISTS (SELECT 1 FROM community_like l WHERE l.post_id = p.id AND l.user_id = #{userId})
                        THEN 1 ELSE 0 END AS isLiked
            FROM community_favorite f
            INNER JOIN community_post p ON p.id = f.post_id
            LEFT JOIN user u ON u.id = p.author_id
            WHERE f.user_id = #{userId} AND p.status = 0
            ORDER BY f.created_at DESC
            """)
    @Results(id = "favListMap", value = {
            @Result(column = "images", property = "images", typeHandler = JacksonTypeHandler.class),
            @Result(column = "summary_raw", property = "summary")
    })
    IPage<PostResponseDTO> selectFavoritePage(Page<PostResponseDTO> page, @Param("userId") Long userId);
}
