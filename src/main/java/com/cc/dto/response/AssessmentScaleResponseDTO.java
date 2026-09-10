package com.cc.dto.response;

import lombok.Data;

/**
 * 量表列表项响应 DTO（测评广场使用）
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentScaleResponseDTO {

    // 量表ID
    private Long id;

    // 量表编码
    private String code;

    // 量表名称
    private String name;

    // 类别
    private String category;

    // 量表简介
    private String description;

    // 题目数
    private Integer questionCount;

    // 预计时长(分钟)
    private Integer estimatedMinutes;
}
