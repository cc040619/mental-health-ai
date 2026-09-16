package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告响应 DTO（用户端携带 isRead，管理端携带 status）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class AnnouncementResponseDTO {

    private Long id;

    private String title;

    private String content;

    // 1-发布中 0-已下架（管理端列表使用）
    private Integer status;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    // 当前用户是否已读（用户端列表使用）
    private Boolean isRead;
}
