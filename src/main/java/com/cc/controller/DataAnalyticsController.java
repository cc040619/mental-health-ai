package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.response.AppointmentStatsResponseDTO;
import com.cc.dto.response.AssessmentStatsResponseDTO;
import com.cc.dto.response.ConsultationStatsResponseDTO;
import com.cc.dto.response.ContentStatsResponseDTO;
import com.cc.dto.response.DashboardOverviewResponseDTO;
import com.cc.dto.response.EmotionStatsResponseDTO;
import com.cc.dto.response.UserTrendResponseDTO;
import com.cc.service.DataAnalyticsService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析控制器（管理员后台数据看板）
 *
 * <p>全部接口仅管理员（user_type=2）可访问；range=TODAY/7D/30D/CUSTOM，
 * CUSTOM 必须传 startDate/endDate（yyyy-MM-dd，跨度 ≤ 1 年），校验在 Service 统一处理。
 * 常用范围命中 Redis 缓存，CUSTOM 实时计算。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/data-analytics")
public class DataAnalyticsController {

    @Resource
    private DataAnalyticsService dataAnalyticsService;

    /** 核心指标卡（六项指标含环比/同比，同时保留旧版概览字段） */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('2')")
    public Result<DashboardOverviewResponseDTO> overview(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(dataAnalyticsService.overview(range, startDate, endDate));
    }

    /** 用户增长趋势：新增用户 + 累计用户，granularity=day/month */
    @GetMapping("/user-trend")
    @PreAuthorize("hasRole('2')")
    public Result<UserTrendResponseDTO> userTrend(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "day") String granularity) {
        return Result.ok(dataAnalyticsService.userTrend(range, startDate, endDate, granularity));
    }

    /** 咨询统计：会话趋势 + 平均时长 + 主题分布 + 情绪分布 */
    @GetMapping("/consultation-stats")
    @PreAuthorize("hasRole('2')")
    public Result<ConsultationStatsResponseDTO> consultationStats(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(dataAnalyticsService.consultationStats(range, startDate, endDate));
    }

    /** 测评分析：量表次数 + 等级分布 + 高危用户预警 TOP10 */
    @GetMapping("/assessment-stats")
    @PreAuthorize("hasRole('2')")
    public Result<AssessmentStatsResponseDTO> assessmentStats(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(dataAnalyticsService.assessmentStats(range, startDate, endDate));
    }

    /** 情绪大数据：主导情绪分布 + 近30天情绪热力图 */
    @GetMapping("/emotion-stats")
    @PreAuthorize("hasRole('2')")
    public Result<EmotionStatsResponseDTO> emotionStats(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(dataAnalyticsService.emotionStats(range, startDate, endDate));
    }

    /** 内容排行：文章/课程/帖子 TOP10（全量累计口径，不随范围筛选） */
    @GetMapping("/content-stats")
    @PreAuthorize("hasRole('2')")
    public Result<ContentStatsResponseDTO> contentStats() {
        return Result.ok(dataAnalyticsService.contentStats());
    }

    /** 预约统计：预约量趋势 + 咨询师排行 + 完成率 */
    @GetMapping("/appointment-stats")
    @PreAuthorize("hasRole('2')")
    public Result<AppointmentStatsResponseDTO> appointmentStats(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(dataAnalyticsService.appointmentStats(range, startDate, endDate));
    }
}
