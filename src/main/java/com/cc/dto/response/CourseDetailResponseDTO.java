package com.cc.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 课程详情响应 DTO
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseDetailResponseDTO {

    // 课程ID
    private Long id;

    // 课程标题
    private String title;

    // 类别 code
    private String category;

    // 类别中文名
    private String categoryLabel;

    // 课程介绍
    private String description;

    // 封面图
    private String coverUrl;

    // 总时长(格式化)
    private String totalDurationText;

    // 学习人数
    private Integer learnCount;

    // 课程完成度百分比(0-100)
    private Integer completion;

    // 当前用户是否已收藏
    private Boolean isFavorite;

    // 评论总数
    private Long commentCount;

    // 章节列表（含进度）
    private List<CourseChapterResponseDTO> chapters;
}
