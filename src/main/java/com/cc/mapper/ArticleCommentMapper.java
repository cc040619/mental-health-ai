package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.ArticleCommentResponseDTO;
import com.cc.entity.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文章评论表 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    /**
     * 管理端分页查询评论（关联文章标题、用户昵称/头像，支持状态与文章筛选）
     *
     * @param page      分页参数
     * @param status    评论状态 0/1/2，null 表示全部
     * @param articleId 文章ID，null 表示全部
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT cm.id, cm.article_id AS articleId, cm.content, cm.status, cm.created_at AS createdAt,
                   a.title AS articleTitle, u.id AS userId, u.nickname AS userName, u.avatar AS userAvatar
            FROM article_comment cm
            LEFT JOIN knowledge_article a ON a.id = cm.article_id
            LEFT JOIN user u ON u.id = cm.user_id
            <where>
                <if test="status != null">AND cm.status = #{status}</if>
                <if test="articleId != null and articleId != ''">AND cm.article_id = #{articleId}</if>
            </where>
            ORDER BY cm.created_at DESC
            </script>
            """)
    IPage<ArticleCommentResponseDTO> selectCommentPage(Page<ArticleCommentResponseDTO> page,
                                                       @Param("status") Integer status,
                                                       @Param("articleId") String articleId);

    /**
     * 前台查询某文章已通过评论列表（status=1，分页）
     *
     * @param page      分页参数
     * @param articleId 文章ID
     * @return 分页结果
     */
    @Select("""
            SELECT cm.id, cm.article_id AS articleId, cm.content, cm.status, cm.created_at AS createdAt,
                   u.id AS userId, u.nickname AS userName, u.avatar AS userAvatar
            FROM article_comment cm
            LEFT JOIN user u ON u.id = cm.user_id
            WHERE cm.article_id = #{articleId} AND cm.status = 1
            ORDER BY cm.created_at DESC
            """)
    IPage<ArticleCommentResponseDTO> selectApprovedPage(Page<ArticleCommentResponseDTO> page,
                                                         @Param("articleId") String articleId);
}
