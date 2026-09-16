package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 看板核心指标卡响应 DTO，六项指标各带环比/同比；
 * 同时保留旧版 overview 的 systemOverview/emotionTrend/consultationStats/userActivity 字段，保证旧前端不破坏。
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponseDTO {

    // 用户总数（环比取本周期新增用户环比，同比对比一年前用户总量）
    private MetricItem totalUsers;

    // 今日新增用户（环比=今日 vs 昨日，同比=今日 vs 去年同日）
    private MetricItem todayNewUsers;

    // 活跃用户（范围内有日记/会话/测评/预约任一操作的去重用户数）
    private MetricItem activeUsers;

    // 咨询会话数（范围内）
    private MetricItem sessions;

    // 测评完成数（范围内 COMPLETED 记录数）
    private MetricItem assessments;

    // 预约数（范围内新增预约）
    private MetricItem appointments;

    // ==================== 旧版字段（保持兼容，结构不变） ====================

    private DataAnalyticsResponseDTO.SystemOverview systemOverview;

    private List<DataAnalyticsResponseDTO.EmotionTrendItem> emotionTrend;

    private DataAnalyticsResponseDTO.ConsultationStats consultationStats;

    private List<DataAnalyticsResponseDTO.UserActivityItem> userActivity;

    /**
     * 指标项：当前值 + 环比百分比 + 同比百分比（基线为 0/数据不足时为 null，前端展示「—」）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricItem {
        // 当前值
        private Long value;
        // 环比百分比（上一同长周期），如 12.5 表示 12.5%
        private Double mom;
        // 同比百分比（去年同期）
        private Double yoy;
    }
}
