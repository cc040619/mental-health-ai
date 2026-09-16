package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("consultation_session")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSession {
    // 会话ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 会话标题
    @Size(max = 200, message = "会话标题长度不能超过200个字符")
    @TableField("session_title")
    private String sessionTitle;

    // 咨询场景：EMOTION/RELATIONSHIP/ACADEMIC/SLEEP/OTHER
    @TableField("scene")
    private String scene;

    // 开始时间
    @TableField("started_at")
    private LocalDateTime startedAt;

    // 最后一次情绪分析结果(JSON格式)
    @TableField("last_emotion_analysis")
    private String lastEmotionAnalysis;

    // 最后一次情绪分析更新时间
    @TableField("last_emotion_updated_at")
    private LocalDateTime lastEmotionUpdatedAt;

    // 最近主情绪标签（冗余，情绪分析写路径同步写入，供后台列表展示与筛选）
    @Size(max = 50, message = "情绪标签长度不能超过50个字符")
    @TableField("emotion_tag")
    private String emotionTag;

    // 高质量标记 0-否 1-是（AI训练参考，人工运营标记）
    @TableField("is_high_quality")
    private Integer isHighQuality;

    // 危机会话标记 0-否 1-是（运营关注标记，与用户端自动危机识别/干预卡解耦）
    @TableField("is_crisis")
    private Integer isCrisis;

    // 标记备注
    @TableField("mark_note")
    private String markNote;

    // 会话总结(JSON格式：summary/coreIssues/suggestions/generatedAt)
    @TableField("summary")
    private String summary;

    // 用户评分 1-5
    @TableField("rating")
    private Integer rating;

    // 用户文字反馈
    @TableField("feedback")
    private String feedback;
}
