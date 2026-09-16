package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端课程详情响应 DTO
 *
 * <p>包含完整基本信息 + 章节列表（按 sortOrder 升序），章节无进度信息（管理员视角）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminCourseDetailResponseDTO {

    private Long id;
    private String title;
    private String category;
    private String categoryLabel;
    private String description;
    private String coverUrl;
    private String lecturer;
    private String targetAudience;
    private Integer totalDuration;
    private String totalDurationText;
    private Integer learnCount;

    private Integer status;
    private String statusLabel;
    private String rejectReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AdminChapterResponseDTO> chapters;
}
