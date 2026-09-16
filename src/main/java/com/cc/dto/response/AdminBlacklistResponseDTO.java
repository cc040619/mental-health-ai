package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端黑名单列表项 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminBlacklistResponseDTO {

    private Long id;
    private Long userId;
    private String nickname;
    private String reason;
    private LocalDateTime createdAt;
}
