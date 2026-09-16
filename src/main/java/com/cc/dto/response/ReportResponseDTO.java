package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区举报响应 DTO（后台举报管理列表）
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class ReportResponseDTO {

    private Long id;

    // 1:帖子 2:评论
    private Integer targetType;
    private Long targetId;

    // 举报人信息
    private Long reporterId;
    private String reporterName;

    private String reason;

    // 0:待处理 1:已下架 2:已忽略 3:已警告
    private Integer status;

    // 举报对象快照（帖子标题 / 评论内容片段）
    private String postTitle;
    private String commentContent;

    // 被举报内容作者昵称（仅举报处理场景可见真实身份）
    private String authorName;

    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
