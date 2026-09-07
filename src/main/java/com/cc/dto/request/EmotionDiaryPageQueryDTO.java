package com.cc.dto.request;

import lombok.Data;

/**
 * 情绪日记后台分页查询 DTO，参数名与前端 emotional.vue 保持一致
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class EmotionDiaryPageQueryDTO {

    // 页码
    private Long current = 1L;

    // 每页条数
    private Long size = 10L;

    // 用户ID筛选
    private Long userId;

    // 情绪评分范围，如 "1-3"、"4-6"、"7-10"
    private String moodScreRange;

    // 情绪评分范围下限（由 Service 解析 moodScreRange 后填充，不接收前端参数）
    private Integer moodScoreMin;

    // 情绪评分范围上限（由 Service 解析 moodScreRange 后填充，不接收前端参数）
    private Integer moodScoreMax;
}
