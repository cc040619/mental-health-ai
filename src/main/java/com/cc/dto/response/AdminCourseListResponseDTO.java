package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端课程列表项响应 DTO
 *
 * <p>章节数/总时长由 SQL LEFT JOIN GROUP BY 一次聚合，禁止 N+1。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminCourseListResponseDTO {

    private Long id;
    private String title;
    private String category;
    private String categoryLabel;
    private String coverUrl;
    private String lecturer;

    // 章节数（聚合查询）
    private Integer chapterCount;

    // 总时长(秒)
    private Integer totalDuration;

    // 总时长(格式化)
    private String totalDurationText;

    // 学习人数
    private Integer learnCount;

    // 状态：0草稿/1待审核/2已发布/3已驳回/4已下架
    private Integer status;
    private String statusLabel;

    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
