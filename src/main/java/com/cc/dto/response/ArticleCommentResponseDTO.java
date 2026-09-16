package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章评论响应 DTO（管理端与前台共用，前台仅展示 status=1 的评论）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ArticleCommentResponseDTO {

    // 评论ID
    private Long id;

    // 文章ID
    private String articleId;

    // 文章标题（管理端列表展示）
    private String articleTitle;

    // 用户ID
    private Long userId;

    // 用户昵称
    private String userName;

    // 用户头像
    private String userAvatar;

    // 评论内容
    private String content;

    // 状态 0:待审核 1:已通过 2:已拒绝
    private Integer status;

    // 评论时间
    private LocalDateTime createdAt;
}
