package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据分析概览响应 DTO，结构与前端 dashboard.vue 渲染所需的字段一一对应
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataAnalyticsResponseDTO {

    // 系统概览
    private SystemOverview systemOverview;

    // 情绪趋势（近7天）
    private List<EmotionTrendItem> emotionTrend;

    // 咨询统计
    private ConsultationStats consultationStats;

    // 用户活跃度（近7天）
    private List<UserActivityItem> userActivity;

    /**
     * 系统概览
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemOverview {
        // 用户总数
        private Long totalUsers;
        // 活跃用户数（近7天有日记或咨询记录的独立用户数）
        private Long activeUsers;
        // 日记总数
        private Long totalDiaries;
        // 今日新增日记数
        private Long todayNewDiaries;
        // 会话总数
        private Long totalSessions;
        // 今日新增会话数
        private Long todayNewSessions;
        // 平均情绪评分(1-10)
        private Double avgMoodScore;
    }

    /**
     * 情绪趋势项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionTrendItem {
        // 日期 yyyy-MM-dd
        private String date;
        // 平均情绪评分
        private Double avgMoodScore;
        // 记录数量
        private Long recordCount;
    }

    /**
     * 咨询统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultationStats {
        // 会话总数
        private Long totalSessions;
        // 平均咨询时长（分钟）
        private Double avgDurationMinutes;
        // 每日趋势
        private List<DailyTrendItem> dailyTrend;
    }

    /**
     * 咨询每日趋势项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrendItem {
        // 日期 yyyy-MM-dd
        private String date;
        // 会话数
        private Long sessionCount;
        // 参与用户数
        private Long userCount;
    }

    /**
     * 用户活跃度项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityItem {
        // 日期 yyyy-MM-dd
        private String date;
        // 活跃用户数
        private Long activeUsers;
        // 新增用户数
        private Long newUsers;
        // 写日记用户数
        private Long diaryUsers;
        // 咨询用户数
        private Long consultationUsers;
    }
}
