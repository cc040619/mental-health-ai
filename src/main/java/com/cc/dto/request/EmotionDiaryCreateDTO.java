package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 添加情绪日记请求 DTO，字段与前端 emotionDiary.vue 表单一一对应
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class EmotionDiaryCreateDTO {

    // 日记日期 YYYY-MM-DD
    @NotNull(message = "日记日期不能为空")
    private LocalDate diaryDate;

    // 情绪评分 1-10
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分必须在1到10之间")
    @Max(value = 10, message = "情绪评分必须在1到10之间")
    private Integer moodScore;

    // 主要情绪（开心/平静/焦虑/悲伤/兴奋/疲惫/惊讶/困惑）
    @NotBlank(message = "主要情绪不能为空")
    @Size(max = 50, message = "主要情绪长度不能超过50个字符")
    private String dominantEmotion;

    // 情绪触发因素
    @Size(max = 500, message = "情绪触发因素长度不能超过500个字符")
    private String emotionTriggers;

    // 日记内容
    @Size(max = 5000, message = "日记内容长度不能超过5000个字符")
    private String diaryContent;

    // 睡眠质量 1-5
    @Min(value = 1, message = "睡眠质量必须在1到5之间")
    @Max(value = 5, message = "睡眠质量必须在1到5之间")
    private Integer sleepQuality;

    // 压力水平 1-5
    @Min(value = 1, message = "压力水平必须在1到5之间")
    @Max(value = 5, message = "压力水平必须在1到5之间")
    private Integer stressLevel;
}
