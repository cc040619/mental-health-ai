package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理端用户统计响应 DTO（一次性聚合返回三组数据）
 *
 * <p>结构直接适配 ECharts：
 * <ul>
 *   <li>genderDistribution 饼图（name 男/女/未知）</li>
 *   <li>ageDistribution 柱状图（name 0-18/19-25/26-35/36-50/50+/未知）</li>
 *   <li>registrationTrend 折线图（name=yyyy-MM-dd）</li>
 * </ul>
 * 走 Redis 缓存（TTL 10 分钟），冷启动回源 DB 一次聚合。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class AdminUserStatsResponseDTO {

    private List<NameCountDTO> genderDistribution;

    private List<NameCountDTO> ageDistribution;

    private List<NameCountDTO> registrationTrend;
}
