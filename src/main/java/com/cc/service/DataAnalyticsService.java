package com.cc.service;

import com.cc.dto.response.DataAnalyticsResponseDTO;
import com.cc.mapper.DataAnalyticsMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析业务层，聚合各统计 SQL 并组装为前端 dashboard 所需的结构
 *
 * @author cc
 * @date 2026-09-06
 */
@Service
public class DataAnalyticsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private DataAnalyticsMapper dataAnalyticsMapper;

    /**
     * 获取数据分析概览
     *
     * @return 概览数据（含系统概览、近7天情绪趋势、咨询统计、近7天用户活跃度）
     */
    public DataAnalyticsResponseDTO overview() {
        DataAnalyticsResponseDTO.SystemOverview systemOverview = buildSystemOverview();
        List<DataAnalyticsResponseDTO.EmotionTrendItem> emotionTrend = buildEmotionTrend();
        DataAnalyticsResponseDTO.ConsultationStats consultationStats = buildConsultationStats();
        List<DataAnalyticsResponseDTO.UserActivityItem> userActivity = buildUserActivity();

        return DataAnalyticsResponseDTO.builder()
                .systemOverview(systemOverview)
                .emotionTrend(emotionTrend)
                .consultationStats(consultationStats)
                .userActivity(userActivity)
                .build();
    }

    /**
     * 系统概览统计
     */
    private DataAnalyticsResponseDTO.SystemOverview buildSystemOverview() {
        return DataAnalyticsResponseDTO.SystemOverview.builder()
                .totalUsers(dataAnalyticsMapper.countUsers())
                .activeUsers(dataAnalyticsMapper.countActiveUsers())
                .totalDiaries(dataAnalyticsMapper.countDiaries())
                .todayNewDiaries(dataAnalyticsMapper.countTodayNewDiaries())
                .totalSessions(dataAnalyticsMapper.countSessions())
                .todayNewSessions(dataAnalyticsMapper.countTodayNewSessions())
                .avgMoodScore(dataAnalyticsMapper.avgMoodScore())
                .build();
    }

    /**
     * 近7天情绪趋势，缺失日期补零，保证图表 X 轴连续
     */
    private List<DataAnalyticsResponseDTO.EmotionTrendItem> buildEmotionTrend() {
        Map<String, Map<String, Object>> trendMap = toKeyedMap(dataAnalyticsMapper.selectEmotionTrend());
        List<DataAnalyticsResponseDTO.EmotionTrendItem> items = new ArrayList<>();
        for (LocalDate date : lastSevenDays()) {
            String key = date.format(DATE_FORMATTER);
            Map<String, Object> row = trendMap.get(key);
            items.add(DataAnalyticsResponseDTO.EmotionTrendItem.builder()
                    .date(key)
                    .avgMoodScore(row != null ? toDouble(row.get("avgMoodScore")) : 0.0)
                    .recordCount(row != null ? toLong(row.get("recordCount")) : 0L)
                    .build());
        }
        return items;
    }

    /**
     * 咨询统计（近7天每日趋势，缺失日期补零）
     */
    private DataAnalyticsResponseDTO.ConsultationStats buildConsultationStats() {
        Map<String, Map<String, Object>> trendMap = toKeyedMap(dataAnalyticsMapper.selectConsultationDailyTrend());
        List<DataAnalyticsResponseDTO.DailyTrendItem> dailyTrend = new ArrayList<>();
        for (LocalDate date : lastSevenDays()) {
            String key = date.format(DATE_FORMATTER);
            Map<String, Object> row = trendMap.get(key);
            dailyTrend.add(DataAnalyticsResponseDTO.DailyTrendItem.builder()
                    .date(key)
                    .sessionCount(row != null ? toLong(row.get("sessionCount")) : 0L)
                    .userCount(row != null ? toLong(row.get("userCount")) : 0L)
                    .build());
        }

        return DataAnalyticsResponseDTO.ConsultationStats.builder()
                .totalSessions(dataAnalyticsMapper.countSessions())
                .avgDurationMinutes(dataAnalyticsMapper.avgConsultationDurationMinutes())
                .dailyTrend(dailyTrend)
                .build();
    }

    /**
     * 近7天用户活跃度（活跃/新增/写日记/咨询用户），缺失日期补零
     */
    private List<DataAnalyticsResponseDTO.UserActivityItem> buildUserActivity() {
        Map<String, Map<String, Object>> activeMap = toKeyedMap(dataAnalyticsMapper.selectDailyActiveUsers());
        Map<String, Map<String, Object>> newUsersMap = toKeyedMap(dataAnalyticsMapper.selectDailyNewUsers());
        Map<String, Map<String, Object>> diaryMap = toKeyedMap(dataAnalyticsMapper.selectDailyDiaryUsers());
        Map<String, Map<String, Object>> consultationMap = toKeyedMap(dataAnalyticsMapper.selectDailyConsultationUsers());

        List<DataAnalyticsResponseDTO.UserActivityItem> items = new ArrayList<>();
        for (LocalDate date : lastSevenDays()) {
            String key = date.format(DATE_FORMATTER);
            items.add(DataAnalyticsResponseDTO.UserActivityItem.builder()
                    .date(key)
                    .activeUsers(activeMap.containsKey(key) ? toLong(activeMap.get(key).get("cnt")) : 0L)
                    .newUsers(newUsersMap.containsKey(key) ? toLong(newUsersMap.get(key).get("cnt")) : 0L)
                    .diaryUsers(diaryMap.containsKey(key) ? toLong(diaryMap.get(key).get("cnt")) : 0L)
                    .consultationUsers(consultationMap.containsKey(key) ? toLong(consultationMap.get(key).get("cnt")) : 0L)
                    .build());
        }
        return items;
    }

    /**
     * 将 SQL 聚合结果按 date 字段转为 Map，便于按日期补齐
     */
    private Map<String, Map<String, Object>> toKeyedMap(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            if (date != null) {
                map.put(date.toString(), row);
            }
        }
        return map;
    }

    /**
     * 近7天日期列表（含今天，升序）
     */
    private List<LocalDate> lastSevenDays() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            dates.add(today.minusDays(i));
        }
        return dates;
    }

    /**
     * 数值安全转换（MySQL 聚合结果可能为 BigDecimal/Long/BigInteger）
     */
    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
