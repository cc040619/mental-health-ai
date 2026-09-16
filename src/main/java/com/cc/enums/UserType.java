package com.cc.enums;

import lombok.Getter;

/**
 * 用户类型枚举：普通用户 / 管理员 / 咨询师
 *
 * @author cc
 * @date 2026-09-01
 */
@Getter
public enum UserType {

    USER(1, "普通用户"),
    ADMIN(2, "管理员"),
    CONSULTANT(3, "咨询师");

    private final Integer code;
    private final String description;

    UserType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static UserType fromCode(Integer code) {
        for (UserType type : UserType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的用户类型代码: " + code);
    }

    /**
     * 验证用户类型代码是否有效
     */
    public static boolean isValidCode(Integer code) {
        for (UserType type : UserType.values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
