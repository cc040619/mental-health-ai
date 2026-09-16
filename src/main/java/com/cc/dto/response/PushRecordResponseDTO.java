package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推送批次记录响应 DTO（阅读数实时统计，无冗余字段）
 *
 * <p>口径：送达率=100%（站内消息落库即送达）；
 * 阅读率 = 阅读数 / 目标数，实时按 related_id 关联 notification 统计。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class PushRecordResponseDTO {

    private Long id;

    // 推送类型（通知类型枚举 code）
    private String type;

    private String title;

    private String content;

    // ALL-全体 USERS-指定用户
    private String targetType;

    private Integer targetCount;

    // 已读数（实时统计）
    private Long readCount;

    // 阅读率（0-100，百分比取整）
    private Integer readRate;

    private LocalDateTime createdAt;
}
