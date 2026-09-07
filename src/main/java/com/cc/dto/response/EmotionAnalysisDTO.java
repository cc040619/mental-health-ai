package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情绪分析结果 DTO，同时用于会话情绪分析（consultation_session.last_emotion_analysis）
 * 与日记情绪分析（emotion_diary.ai_emotion_analysis）的 JSON 结构
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisDTO {

    // 主要情绪，如 "焦虑"
    private String primaryEmotion;

    // 情绪评分 0-100
    private Integer emotionScore;

    // 是否为负面情绪
    private Boolean isNegative;

    // 风险等级 0-3
    private Integer riskLevel;

    // 情绪关键词
    private List<String> keywords;

    // 温暖建议
    private String suggestion;

    // 情绪图标（emoji）
    private String icon;

    // 情绪标签（简短）
    private String label;

    // 风险描述
    private String riskDescription;

    // 改善建议列表
    private List<String> improvementSuggestions;

    // 分析时间戳（毫秒）
    private Long timestamp;

    /**
     * 构建默认分析结果（无分析数据时返回，与前端默认值保持一致）
     */
    public static EmotionAnalysisDTO defaultAnalysis() {
        return EmotionAnalysisDTO.builder()
                .primaryEmotion("中性")
                .emotionScore(50)
                .isNegative(false)
                .riskLevel(0)
                .keywords(List.of())
                .suggestion("暂时没有足够的对话内容进行分析，继续聊聊吧")
                .icon("😐")
                .label("平静")
                .riskDescription("情绪稳定")
                .improvementSuggestions(List.of("保持规律作息", "适当运动", "与朋友交流"))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
