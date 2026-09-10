package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏消息响应 DTO（收藏的 AI 回复列表项）
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@Builder
public class FavoriteMessageResponseDTO {

    // 消息ID
    private Long id;

    // 消息内容（AI回复）
    private String content;

    // 所属会话ID
    private Long sessionId;

    // 所属会话标题
    private String sessionTitle;

    // 收藏时间
    private LocalDateTime favoritedAt;
}
