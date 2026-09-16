package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预约统计响应 DTO：预约量每日趋势 + 咨询师预约量排行 TOP10 + 预约完成率
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentStatsResponseDTO {

    // 预约量每日趋势
    private List<NameCountDTO> dailyTrend;

    // 咨询师预约量排行 TOP10
    private List<NameCountDTO> consultantRanking;

    // 预约完成率（百分比）：COMPLETED / (总数 − CANCELED)；分母为 0 时为 null
    private Double completionRate;

    // 各状态占比（name=状态码 PENDING/CONFIRMED/COMPLETED/CANCELED）
    private List<NameCountDTO> statusDistribution;

    // 热门时段（name=小时 0-23，value=预约量）
    private List<NameCountDTO> hotHours;
}
