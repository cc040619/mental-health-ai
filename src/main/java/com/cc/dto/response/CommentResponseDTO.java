package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区评论响应 DTO（顶层评论内嵌 replies 楼中楼回复列表）
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class CommentResponseDTO {

    private Long id;

    private Long postId;

    // 父评论ID（顶层评论为 null）
    private Long parentId;

    // 作者信息（匿名评论对外屏蔽）
    private Long authorId;
    private String authorName;
    private String avatar;
    private Integer isAnonymous;

    private String content;

    // 危机干预提示（评论命中危机词时随发布响应返回，不落库）
    private Object crisisTip;

    // 是否为当前访问者本人发布（用于删除按钮显隐）
    private Boolean isMine;

    private LocalDateTime createdAt;

    // 楼中楼回复（仅顶层评论携带）
    private List<CommentResponseDTO> replies;

    // 所属帖子标题（"我的评论"列表使用）
    private String postTitle;
}
