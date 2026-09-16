package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 咨询数据统计响应 DTO：会话数每日趋势 + 平均会话时长 + 咨询主题分布 + 情绪分布
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationStatsResponseDTO {

    // 会话数每日趋势（缺失日期补零）
    private List<DailyTrend> dailyTrend;

    // 平均会话时长（分钟）：会话最后一条消息时间 − started_at
    private Double avgDurationMinutes;

    // 咨询主题分布（consultation_session.scene 分组；scene 列缺失时为空列表）
    private List<NameCountDTO> sceneDistribution;

    // 情绪分布（consultation_message.emotion_tag 分组）
    private List<NameCountDTO> emotionDistribution;

    /**
     * 会话每日趋势项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        // 日期 yyyy-MM-dd
        private String date;
        // 会话数
        private Long sessionCount;
        // 参与用户数
        private Long userCount;
    }
}
