package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端评论列表项 DTO（列表中匿名评论作者屏蔽为「匿名用户」，真实身份仅举报处理可见）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminCommentResponseDTO {

    private Long id;
    private Long postId;

    // 所属帖子标题
    private String postTitle;

    // 父评论ID（楼中楼时非空）
    private Long parentId;

    // 父评论内容摘要（楼中楼缩进展示用）
    private String parentContent;

    private Long authorId;

    // 匿名评论统一「匿名用户」
    private String authorName;
    private Integer isAnonymous;

    private String content;

    // 0-正常 1-已删除 2-待审核
    private Integer status;

    private LocalDateTime createdAt;
}
