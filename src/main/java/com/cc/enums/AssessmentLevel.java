package com.cc.enums;

import lombok.Getter;

/**
 * 测评结果等级枚举
 *
 * @author cc
 * @date 2026-09-08
 */
@Getter
public enum AssessmentLevel {

    NORMAL("NORMAL", "正常", "#7BC67E"),
    MILD("MILD", "轻度", "#F2C94C"),
    MODERATE("MODERATE", "中度", "#E8927C"),
    SEVERE("SEVERE", "重度", "#D9534F");

    private final String code;
    private final String label;
    private final String color;

    AssessmentLevel(String code, String label, String color) {
        this.code = code;
        this.label = label;
        this.color = color;
    }

    public static AssessmentLevel fromCode(String code) {
        for (AssessmentLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
