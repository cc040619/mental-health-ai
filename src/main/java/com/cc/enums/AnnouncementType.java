package com.cc.enums;

import lombok.Getter;

/**
 * 公告类型枚举（落库 announcement.type）
 *
 * @author cc
 * @date 2026-09-15
 */
@Getter
public enum AnnouncementType {

    SYSTEM("SYSTEM", "系统公告"),
    ACTIVITY("ACTIVITY", "活动通知"),
    TIP("TIP", "温馨提示");

    private final String code;
    private final String label;

    AnnouncementType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AnnouncementType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AnnouncementType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
