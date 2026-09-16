package com.cc.enums;

import com.cc.exception.BusinessException;
import lombok.Getter;

/**
 * 数据看板统计时间范围枚举
 *
 * @author cc
 * @date 2026-09-15
 */
@Getter
public enum StatsRange {

    TODAY("TODAY", "今日"),
    D7("7D", "近7天"),
    D30("30D", "近30天"),
    CUSTOM("CUSTOM", "自定义");

    private final String code;
    private final String label;

    StatsRange(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 解析范围编码，空值默认近7天；无法识别的非空值抛业务异常
     */
    public static StatsRange fromCode(String code) {
        if (code == null || code.isBlank()) {
            return D7;
        }
        for (StatsRange range : values()) {
            if (range.code.equals(code)) {
                return range;
            }
        }
        throw new BusinessException("时间范围参数不正确");
    }
}
