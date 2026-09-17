package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个人资料响应 DTO（个人中心使用，包含个人简介）
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
@Builder
public class ProfileResponseDTO {

    // 用户ID
    private Long id;

    // 用户名
    private String username;

    // 邮箱
    private String email;

    // 昵称
    private String nickname;

    // 头像URL
    private String avatar;

    // 手机号
    private String phone;

    // 性别
    private Integer gender;

    // 性别显示名称
    private String genderDisplayName;

    // 生日
    private LocalDate birthday;

    // 个人简介
    private String bio;

    // 显示名称
    private String displayName;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    /** 是否已设置密码（password 非空）；新注册邮箱用户为 false，需引导设置密码 */
    private Boolean hasPassword;
}
