package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端用户详情响应 DTO
 *
 * <p>展示完整手机号与邮箱；同时携带简要统计供详情页概览使用。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class AdminUserDetailResponseDTO {

    // ===== 基本信息 =====
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer gender;
    private String genderDisplayName;
    private LocalDate birthday;
    private Integer userType;
    private String userTypeDisplayName;
    private Integer status;
    private String statusDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // ===== 简要统计 =====
    private long diaryCount;
    private long sessionCount;
    private long assessmentCount;
}
