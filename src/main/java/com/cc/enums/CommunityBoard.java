package com.cc.enums;

import lombok.Getter;

/**
 * 社区板块枚举
 *
 * @author cc
 * @date 2026-09-10
 */
@Getter
public enum CommunityBoard {

    TREES("TREES", "情绪树洞"),
    ACADEMIC("ACADEMIC", "学业压力"),
    RELATIONSHIP("RELATIONSHIP", "人际关系"),
    SLEEP("SLEEP", "睡眠困扰"),
    RECOVERY("RECOVERY", "康复分享"),
    HELP("HELP", "互助问答");

    private final String code;
    private final String label;

    CommunityBoard(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static CommunityBoard fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CommunityBoard board : values()) {
            if (board.code.equals(code)) {
                return board;
            }
        }
        return null;
    }
}
