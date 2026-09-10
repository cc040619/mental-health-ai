package com.cc.dto.response;

import lombok.Data;

/**
 * 课程章节响应 DTO（含当前用户的学习进度）
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseChapterResponseDTO {

    // 章节ID
    private Long id;

    // 章节标题
    private String title;

    // 音频地址
    private String audioUrl;

    // 时长(秒)
    private Integer duration;

    // 时长(格式化)
    private String durationText;

    // 章节图文介绍
    private String intro;

    // 排序
    private Integer sortOrder;

    // 当前用户已播秒数（续播位置）
    private Integer progressSeconds;

    // 当前用户是否已完成该章
    private Boolean isFinished;
}
