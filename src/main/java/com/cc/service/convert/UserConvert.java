package com.cc.service.convert;

import com.cc.dto.request.ProfileUpdateDTO;
import com.cc.dto.request.UserRegisterCommandDTO;
import com.cc.dto.response.AdminUserDetailResponseDTO;
import com.cc.dto.response.AdminUserListResponseDTO;
import com.cc.dto.response.ProfileResponseDTO;
import com.cc.dto.response.UserLoginResponseDTO;
import com.cc.entity.User;
import com.cc.enums.UserStatus;


import java.time.LocalDateTime;

/**
 * 用户对象转换工具类，用于实体与 DTO 之间的转换
 *
 * @author cc
 * @date 2026-09-01
 */
public class UserConvert {
    // 构建响应DTO
    public static UserLoginResponseDTO.UserDetailResponseDTO entityToDetailResponse(User user) {
        return UserLoginResponseDTO.UserDetailResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .birthday(user.getBirthday())
                .userType(user.getUserType())
                .userTypeDisplayName(user.getUserTypeDisplayName())
                .status(user.getStatus())
                .statusDisplayName(user.getStatusDisplayName())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 构建个人中心资料响应 DTO（含个人简介）
     *
     * @param user 用户实体
     * @return 个人资料响应
     */
    public static ProfileResponseDTO entityToProfileResponse(User user) {
        return ProfileResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .birthday(user.getBirthday())
                .bio(user.getBio())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static UserLoginResponseDTO entityToLoginResponse(String token, UserLoginResponseDTO.UserDetailResponseDTO userInfo) {
        return UserLoginResponseDTO.builder()
                .userInfo(userInfo)
                .token(token)
                .roleType(userInfo.getUserType().toString())
                .build();
    }

    /**
     * 构建管理端用户列表项 DTO（手机号脱敏 138****1234）
     */
    public static AdminUserListResponseDTO entityToAdminListResponse(User user) {
        return AdminUserListResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(maskPhone(user.getPhone()))
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .status(user.getStatus())
                .statusDisplayName(user.getStatusDisplayName())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * 构建管理端用户详情 DTO（含完整手机号与邮箱），简要统计由 Service 填充
     */
    public static AdminUserDetailResponseDTO entityToAdminDetailResponse(User user,
                                                                        long diaryCount,
                                                                        long sessionCount,
                                                                        long assessmentCount) {
        return AdminUserDetailResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .birthday(user.getBirthday())
                .userType(user.getUserType())
                .userTypeDisplayName(user.getUserTypeDisplayName())
                .status(user.getStatus())
                .statusDisplayName(user.getStatusDisplayName())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .diaryCount(diaryCount)
                .sessionCount(sessionCount)
                .assessmentCount(assessmentCount)
                .build();
    }

    /**
     * 手机号脱敏 138****1234；非 11 位号码原样返回（不臆造掩码）
     */
    private static String maskPhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.matches("\\d{11}") ? phone.substring(0, 3) + "****" + phone.substring(7) : phone;
    }

    public static User registerCommandToEntity(UserRegisterCommandDTO commandDTO, String encodedPassword) {
        return User.builder()
                .username(commandDTO.getUsername())
                .email(commandDTO.getEmail())
                .password(encodedPassword)
                .nickname(commandDTO.getNickname())
                .phone(commandDTO.getPhone())
                .gender(commandDTO.getGender())
                .birthday(commandDTO.getBirthday())
                .userType(commandDTO.getUserType())
                .status(UserStatus.NORMAL.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 获取性别显示名称
     * @param gender 性别代码
     * @return 性别显示名称
     */
    private static String getGenderDisplayName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }
    }
}
