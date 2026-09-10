package com.cc.enums;

import lombok.Getter;

/**
 * 社区帖子状态枚举
 *
 * @author cc
 * @date 2026-09-10
 */
@Getter
public enum CommunityPostStatus {

    NORMAL(0, "正常"),
    DELETED(1, "已删除"),
    PENDING_REVIEW(2, "举报待审"),
    TAKEN_DOWN(3, "已下架");

    private final int code;
    private final String label;

    CommunityPostStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
