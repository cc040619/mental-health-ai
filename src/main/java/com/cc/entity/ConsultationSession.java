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
