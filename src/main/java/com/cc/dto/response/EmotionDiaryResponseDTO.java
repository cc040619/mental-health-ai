package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记响应 DTO（后台分页列表使用），字段与前端 emotional.vue 读取的属性一一对应
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class EmotionDiaryResponseDTO {

    // 日记ID
    private Long id;

    // 用户ID
    private Long userId;

    // 用户昵称
    private String nickname;

    // 用户名
    private String username;

    // 日记日期
    private LocalDate diaryDate;

    // 情绪评分(1-10)
    private Integer moodScore;

    // 主要情绪
    private String dominantEmotion;

    // 情绪触发因素
    private String emotionTriggers;

    // 日记内容
    private String diaryContent;

    // 睡眠质量(1-5)
    private Integer sleepQuality;

    // 压力水平(1-5)
    private Integer stressLevel;

    // AI情绪分析结果（JSON字符串，前端 JSON.parse 后读取 primaryEmotion 等字段）
    private String aiEmotionAnalysis;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
