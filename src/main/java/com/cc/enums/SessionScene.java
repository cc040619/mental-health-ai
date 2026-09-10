package com.cc.enums;

import lombok.Getter;

/**
 * 咨询会话场景枚举
 *
 * <p>新建会话时由用户选择，AI 据此叠加对应场景的系统提示词，调整回复风格。</p>
 *
 * @author cc
 * @date 2026-09-09
 */
@Getter
public enum SessionScene {

    EMOTION("EMOTION", "情绪疏导", "🌈"),
    RELATIONSHIP("RELATIONSHIP", "人际关系", "🤝"),
    ACADEMIC("ACADEMIC", "学业压力", "📚"),
    SLEEP("SLEEP", "睡眠问题", "🌙"),
    OTHER("OTHER", "其他", "💬");

    private final String code;
    private final String label;
    private final String icon;

    SessionScene(String code, String label, String icon) {
        this.code = code;
        this.label = label;
        this.icon = icon;
    }

    /**
     * 按 code 解析场景枚举，非法值返回 null
     */
    public static SessionScene fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SessionScene scene : values()) {
            if (scene.code.equals(code)) {
                return scene;
            }
        }
        return null;
    }
}
