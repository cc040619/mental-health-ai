package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 课程学习数据统计 DTO
 *
 * <p>口径定义：</p>
 * <ul>
 *   <li>learnerCount = course.learn_count 冗余字段（首次学习 +1）</li>
 *   <li>completionRate = 学完全部章节的用户数 ÷ 学习人数 × 100（百分比，0-100）</li>
 *   <li>avgProgressSeconds = SUM(progress_seconds) ÷ COUNT(DISTINCT user_id)</li>
 *   <li>chapterHeatmap = 每章 {学习人数, 完成人数, 平均进度}，一次 JOIN 聚合</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class CourseStatsResponseDTO {

    private Integer learnerCount;
    private Double completionRate;
    private Integer avgProgressSeconds;
    private List<ChapterHeatmapItem> chapterHeatmap;

    @Data
    @Builder
    public static class ChapterHeatmapItem {
        private Long chapterId;
        private String title;
        private Integer sortOrder;
        private Integer learnerCount;
        private Integer finishCount;
        private Integer avgProgress;
    }
}
