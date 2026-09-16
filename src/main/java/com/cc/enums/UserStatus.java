package com.cc.enums;

import lombok.Getter;

/**
 * 用户状态枚举：禁用 / 正常 / 待审核
 *
 * @author cc
 * @date 2026-09-01
 */
@Getter
public enum UserStatus {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常"),
    /** 注册审核开启时新用户的初始状态，管理端在用户管理中启用后转为正常 */
    PENDING(2, "待审核");

    private final Integer code;
    private final String description;

    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static UserStatus fromCode(Integer code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态代码: " + code);
    }
}
