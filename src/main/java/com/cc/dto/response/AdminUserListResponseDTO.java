package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表项响应 DTO
 *
 * <p>手机号在转换层脱敏（138****1234），邮箱不在列表展示，仅详情可见。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class AdminUserListResponseDTO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    // 脱敏后的手机号（138****1234）
    private String phone;

    private Integer gender;

    private String genderDisplayName;

    private Integer status;

    private String statusDisplayName;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;
}
