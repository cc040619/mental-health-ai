package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
}
