package com.cc.dto.response;

import lombok.Data;

/**
 * 每日一练推荐响应 DTO
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class DailyPracticeResponseDTO {

    // 课程ID
    private Long courseId;

    // 课程标题
    private String courseTitle;

    // 课程封面
    private String courseCover;

    // 类别 code
    private String category;

    // 类别中文名
    private String categoryLabel;

    // 章节ID
    private Long chapterId;

    // 章节标题
    private String chapterTitle;

    // 音频地址
    private String audioUrl;

    // 时长(秒)
    private Integer duration;

    // 时长(格式化)
    private String durationText;

    // 章节图文介绍
    private String intro;

    // 今日是否已打卡
    private Boolean hasCheckedInToday;
}
