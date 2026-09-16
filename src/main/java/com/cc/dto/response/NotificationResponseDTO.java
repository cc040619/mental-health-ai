package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知响应 DTO
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class NotificationResponseDTO {

    private Long id;

    // ANNOUNCEMENT/APPOINTMENT/INTERACTION/ASSESSMENT
    private String type;

    private String title;

    private String content;

    // POST/APPOINTMENT/ASSESSMENT
    private String relatedType;

    private Long relatedId;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
