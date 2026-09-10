package com.cc.enums;

import lombok.Getter;

/**
 * 社区举报对象类型枚举
 *
 * @author cc
 * @date 2026-09-10
 */
@Getter
public enum CommunityReportTarget {

    POST(1, "帖子"),
    COMMENT(2, "评论");

    private final int code;
    private final String label;

    CommunityReportTarget(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static CommunityReportTarget fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CommunityReportTarget target : values()) {
            if (target.code == code) {
                return target;
            }
        }
        return null;
    }
}
