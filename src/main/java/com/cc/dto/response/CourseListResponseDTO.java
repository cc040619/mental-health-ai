package com.cc.dto.response;

import lombok.Data;

/**
 * 课程列表项响应 DTO（广场 / 我的收藏 / 我的课程 共用）
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseListResponseDTO {

    // 课程ID
    private Long id;

    // 课程标题
    private String title;

    // 类别 code
    private String category;

    // 类别中文名
    private String categoryLabel;

    // 封面图
    private String coverUrl;

    // 总时长(秒，仅 SQL 映射用)
    private Integer totalDuration;

    // 总时长(格式化为 x分x秒)
    private String totalDurationText;

    // 学习人数
    private Integer learnCount;

    // 当前用户是否已收藏
    private Boolean isFavorite;

    // 当前用户是否已开始学习（有进度记录）
    private Boolean isLearning;

    // 课程完成度百分比(0-100)，广场列表不返回
    private Integer completion;
}
