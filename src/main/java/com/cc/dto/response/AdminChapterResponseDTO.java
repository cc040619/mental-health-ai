package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端章节响应 DTO（无进度信息，仅管理员视角的章节元信息）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminChapterResponseDTO {

    private Long id;
    private Long courseId;
    private String title;
    private String audioUrl;
    private String videoUrl;
    private Integer duration;
    private String durationText;
    private String intro;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
