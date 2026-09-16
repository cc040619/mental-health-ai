package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台咨询会话详情响应 DTO（会话信息 + 消息时间轴 + 总结 + 评价 + 标记）
 *
 * <p>情绪变化折线图数据由 messages 序列直接提供（前端取 senderType=1 且 emotionValue 非空的消息），
 * 不单独出折线接口，避免一次会话两次查询。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class AdminSessionDetailResponseDTO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String sessionTitle;
    private String scene;
    private String sceneLabel;

    private Long durationSeconds;
    private Long msgCount;

    private String emotionTag;
    private Integer rating;
    private String feedback;

    private Integer isHighQuality;
    private Integer isCrisis;
    private String markNote;

    private LocalDateTime startedAt;

    // AI 总结（已反序列化；无总结为 null）
    private SessionSummaryResponseDTO summaryObj;

    // 消息时间轴（按时间升序，含情绪标签/情绪值）
    private List<MessageItem> messages;

    @Data
    @Builder
    public static class MessageItem {
        private Long id;
        // 1-用户 2-AI
        private Integer senderType;
        private String content;
        private String emotionTag;
        private BigDecimal emotionValue;
        private LocalDateTime createdAt;
    }
}
