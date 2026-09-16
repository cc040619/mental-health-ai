package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台咨询会话列表响应 DTO（一次 LEFT JOIN 聚合查询，禁止 N+1）
 *
 * <p>时长/消息数不落库：msg_count/duration_seconds 由聚合 SQL 现算。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminConsultationListResponseDTO {

    private Long id;

    private Long userId;
    private String userNickname;

    private String sessionTitle;

    // 场景编码与中文标签（EMOTION/RELATIONSHIP/ACADEMIC/SLEEP/OTHER）
    private String scene;
    private String sceneLabel;

    // 会话时长（秒 = 最后消息时间 - started_at；无消息为 null，前端显示「—」）
    private Long durationSeconds;

    private Long msgCount;

    // 最近主情绪标签（冗余字段）
    private String emotionTag;

    // 用户评分 1-5
    private Integer rating;

    private Integer isHighQuality;
    private Integer isCrisis;

    private LocalDateTime startedAt;
}
