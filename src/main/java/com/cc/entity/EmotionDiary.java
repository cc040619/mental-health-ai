package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记实体类，对应数据库 emotion_diary 表
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@TableName("emotion_diary")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionDiary {
    // 日记ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 日记日期
    @NotNull(message = "日记日期不能为空")
    @TableField("diary_date")
    private LocalDate diaryDate;

    // 情绪评分(1-10)
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分必须在1到10之间")
    @Max(value = 10, message = "情绪评分必须在1到10之间")
    @TableField("mood_score")
    private Integer moodScore;

    // 主要情绪
    @Size(max = 50, message = "主要情绪长度不能超过50个字符")
    @TableField("dominant_emotion")
    private String dominantEmotion;

    // 情绪触发因素
    @TableField("emotion_triggers")
    private String emotionTriggers;

    // 日记内容
    @TableField("diary_content")
    private String diaryContent;

    // 睡眠质量(1-5)
    @Min(value = 1, message = "睡眠质量必须在1到5之间")
    @Max(value = 5, message = "睡眠质量必须在1到5之间")
    @TableField("sleep_quality")
    private Integer sleepQuality;

    // 压力水平(1-5)
    @Min(value = 1, message = "压力水平必须在1到5之间")
    @Max(value = 5, message = "压力水平必须在1到5之间")
    @TableField("stress_level")
    private Integer stressLevel;

    // AI情绪分析结果(JSON格式)
    @TableField("ai_emotion_analysis")
    private String aiEmotionAnalysis;

    // AI分析更新时间
    @TableField("ai_analysis_updated_at")
    private LocalDateTime aiAnalysisUpdatedAt;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
