package com.cc.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 量表详情响应 DTO（含题目与选项，供作答页渲染）
 * scoring_config 由 AssessmentConvert 解析后填充
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentScaleDetailDTO {

    // 量表ID
    private Long id;

    // 量表编码
    private String code;

    // 量表名称
    private String name;

    // 类别
    private String category;

    // 量表简介（含学术声明）
    private String description;

    // 题目数
    private Integer questionCount;

    // 预计时长(分钟)
    private Integer estimatedMinutes;

    // 题目列表（解析自 scoring_config）
    private List<QuestionOption> questions;

    /**
     * 题目（含选项）
     */
    @Data
    public static class QuestionOption {
        // 题目索引
        private Integer index;
        // 题干
        private String text;
        // 选项列表
        private List<Option> options;
    }

    /**
     * 选项
     */
    @Data
    public static class Option {
        // 选项文案
        private String label;
        // 选项分值
        private Integer score;
    }
}
