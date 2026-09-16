package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminCommentResponseDTO;
import com.cc.dto.response.CommentResponseDTO;
import com.cc.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区评论 Mapper
 *
 * <p>详情页评论分两次查询（顶层分页 + 楼中楼一次全量），内存按 parentId 分组，禁止 N+1。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@Mapper
public interface CommunityCommentMapper extends BaseMapper<CommunityComment> {

    /**
     * 顶层评论分页（parent_id IS NULL）
     */
    @Select("""
            <script>
            SELECT c.id, c.post_id AS postId, c.parent_id AS parentId, c.author_id AS authorId,
                   c.content, c.is_anonymous AS isAnonymous, c.created_at AS createdAt,
                   CASE WHEN c.is_anonymous = 1 AND #{showReal} = 0 THEN '匿名用户'
                        ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS authorName,
                   CASE WHEN c.is_anonymous = 1 AND #{showReal} = 0 THEN NULL ELSE u.avatar END AS avatar,
                   CASE WHEN c.author_id = #{userId} THEN 1 ELSE 0 END AS isMine
            FROM community_comment c
            LEFT JOIN user u ON u.id = c.author_id
            WHERE c.post_id = #{postId} AND c.status = 0 AND c.parent_id IS NULL
            ORDER BY c.created_at DESC
            </script>
            """)
    IPage<CommentResponseDTO> selectTopCommentPage(Page<CommentResponseDTO> page,
                                                   @Param("postId") Long postId,
                                                   @Param("userId") Long userId,
                                                   @Param("showReal") int showReal);

    /**
     * 楼中楼回复全量（某帖所有非顶层评论），内存按 parentId 分组挂到顶层评论
     */
    @Select("""
            SELECT c.id, c.post_id AS postId, c.parent_id AS parentId, c.author_id AS authorId,
                   c.content, c.is_anonymous AS isAnonymous, c.created_at AS createdAt,
                   CASE WHEN c.is_anonymous = 1 AND #{showReal} = 0 THEN '匿名用户'
                        ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS authorName,
                   CASE WHEN c.is_anonymous = 1 AND #{showReal} = 0 THEN NULL ELSE u.avatar END AS avatar,
                   CASE WHEN c.author_id = #{userId} THEN 1 ELSE 0 END AS isMine
            FROM community_comment c
            LEFT JOIN user u ON u.id = c.author_id
            WHERE c.post_id = #{postId} AND c.status = 0 AND c.parent_id IS NOT NULL
            ORDER BY c.created_at ASC
            """)
    List<CommentResponseDTO> selectReplies(@Param("postId") Long postId,
                                           @Param("userId") Long userId,
                                           @Param("showReal") int showReal);

    /**
     * 我的评论列表（携带所属帖子标题）
     */
    @Select("""
            SELECT c.id, c.post_id AS postId, c.parent_id AS parentId, c.author_id AS authorId,
                   c.content, c.is_anonymous AS isAnonymous, c.created_at AS createdAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS authorName,
                   u.avatar, 1 AS isMine,
                   p.title AS postTitle
            FROM community_comment c
            LEFT JOIN user u ON u.id = c.author_id
            LEFT JOIN community_post p ON p.id = c.post_id
            WHERE c.author_id = #{userId} AND c.status = 0
            ORDER BY c.created_at DESC
            LIMIT 100
            """)
    List<CommentResponseDTO> selectMyComments(@Param("userId") Long userId);

    /**
     * 管理端评论分页：帖子/内容关键字/状态筛选，带帖子标题与父评论摘要（楼中楼缩进展示）。
     * 匿名评论作者屏蔽为「匿名用户」，真实身份仅举报处理可见。
     */
    @Select("""
            <script>
            SELECT c.id, c.post_id AS postId, c.parent_id AS parentId, c.author_id AS authorId,
                   c.content, c.is_anonymous AS isAnonymous, c.status, c.created_at AS createdAt,
                   CASE WHEN c.is_anonymous = 1 THEN '匿名用户'
                        ELSE COALESCE(NULLIF(u.nickname, ''), u.username) END AS authorName,
                   p.title AS postTitle,
                   pc.content AS parentContent
            FROM community_comment c
            LEFT JOIN user u ON u.id = c.author_id
            LEFT JOIN community_post p ON p.id = c.post_id
            LEFT JOIN community_comment pc ON pc.id = c.parent_id
            <where>
                <if test='postId != null'>AND c.post_id = #{postId}</if>
                <if test='status != null'>AND c.status = #{status}</if>
                <if test='keyword != null and keyword != ""'>AND c.content LIKE CONCAT('%', #{keyword}, '%')</if>
            </where>
            ORDER BY c.created_at DESC
            </script>
            """)
    IPage<AdminCommentResponseDTO> selectAdminCommentPage(Page<AdminCommentResponseDTO> page,
                                                          @Param("postId") Long postId,
                                                          @Param("keyword") String keyword,
                                                          @Param("status") Integer status);
}
