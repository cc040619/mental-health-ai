package com.cc.enums;

import lombok.Getter;

/**
 * 测评提醒频率枚举（落库 user_notify_setting.assessment_frequency）
 *
 * @author cc
 * @date 2026-09-14
 */
@Getter
public enum AssessmentFrequency {

    NONE("NONE", "关闭"),
    WEEKLY("WEEKLY", "每周"),
    DAILY("DAILY", "每天");

    private final String code;
    private final String label;

    AssessmentFrequency(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AssessmentFrequency fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AssessmentFrequency frequency : values()) {
            if (frequency.code.equals(code)) {
                return frequency;
            }
        }
        return null;
    }
}
