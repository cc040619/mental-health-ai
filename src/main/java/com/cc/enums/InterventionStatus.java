package com.cc.enums;

import lombok.Getter;

/**
 * 高危预警干预状态枚举
 * 0-未处理 / 1-已干预 / 2-已处理
 * 流转：未处理 → 已干预 → 已处理（可回退到已处理前一状态）
 *
 * @author cc
 * @date 2026-09-15
 */
@Getter
public enum InterventionStatus {

    PENDING(0, "未处理"),
    INTERVENED(1, "已干预"),
    RESOLVED(2, "已处理");

    private final Integer code;
    private final String label;

    InterventionStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static InterventionStatus fromCode(Integer code) {
        if (code == null) return null;
        for (InterventionStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
