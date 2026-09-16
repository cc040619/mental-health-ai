package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约留言 DTO（WebSocket 广播与 REST 历史共用同一结构）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class AppointmentMessageResponseDTO {

    private Long id;

    /** 1-用户 2-咨询师/管理员 */
    private Integer senderType;

    private String content;

    private LocalDateTime createdAt;
}
