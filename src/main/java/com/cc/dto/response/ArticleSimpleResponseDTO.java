package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文章分页列表响应 DTO，字段覆盖管理端表格与前端文章卡片所需属性
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class ArticleSimpleResponseDTO {

    // 文章ID(UUID)
    private String id;

    // 文章标题
    private String title;

    // 文章摘要
    private String summary;

    // 封面图片
    private String coverImage;

    // 分类ID
    private Long categoryId;

    // 分类名称
    private String categoryName;

    // 标签（逗号分隔）
    private String tags;

    // 作者名称（昵称）
    private String authorName;

    // 阅读次数
    private Integer readCount;

    // 点赞数
    private Integer likeCount;

    // 已通过评论数
    private Integer commentCount;

    // 收藏数
    private Integer favoriteCount;

    // 状态 0:草稿 1:已发布 2:已下线
    private Integer status;

    // 发布时间
    private LocalDateTime publishedAt;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
