package com.cc.enums;

import lombok.Getter;

/**
 * 消息通知类型枚举（落库 notification.type，varchar）
 *
 * @author cc
 * @date 2026-09-14
 */
@Getter
public enum NotificationType {

    ANNOUNCEMENT("ANNOUNCEMENT", "系统公告"),
    APPOINTMENT("APPOINTMENT", "预约通知"),
    INTERACTION("INTERACTION", "互动通知"),
    ASSESSMENT("ASSESSMENT", "测评提醒"),
    CONSULTANT("CONSULTANT", "咨询师回复");

    private final String code;
    private final String label;

    NotificationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static NotificationType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
