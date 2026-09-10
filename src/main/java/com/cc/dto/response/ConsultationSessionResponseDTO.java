package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询会话响应 DTO，字段与前端 consultation.vue / consultations.vue 读取的属性一一对应
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class ConsultationSessionResponseDTO {

    // 会话ID
    private Long id;

    // 用户ID
    private Long userId;

    // 会话标题
    private String sessionTitle;

    // 咨询场景编码：EMOTION/RELATIONSHIP/ACADEMIC/SLEEP/OTHER
    private String scene;

    // 咨询场景中文名
    private String sceneLabel;

    // 场景 emoji
    private String sceneIcon;

    // 开始时间
    private LocalDateTime startedAt;

    // 最后一条消息内容（列表预览）
    private String lastMessageContent;

    // 消息数量
    private Integer messageCount;

    // 最后一条消息时间
    private LocalDateTime lastMessageTime;

    // 用户昵称（管理端头像列）
    private String userNickname;

    // 用户评分 1-5（未评为 null）
    private Integer rating;

    // 是否已生成会话总结
    private Boolean hasSummary;
}
