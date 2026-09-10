package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浏览历史列表项响应 DTO（携带文章摘要信息）
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class BrowseHistoryResponseDTO {

    // 历史记录ID
    private Long historyId;

    // 文章ID
    private String articleId;

    // 最近浏览时间
    private LocalDateTime browseTime;

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

    // 作者ID
    private Long authorId;

    // 作者名称（昵称）
    private String authorName;

    // 阅读次数
    private Integer readCount;

    // 文章状态 0:草稿 1:已发布 2:已下线
    private Integer articleStatus;

    // 发布时间
    private LocalDateTime publishedAt;
}
