package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测评记录列表项响应 DTO（测评历史使用）
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentRecordResponseDTO {

    // 记录ID
    private Long id;

    // 用户ID
    private Long userId;

    // 量表ID
    private Long scaleId;

    // 总分
    private BigDecimal totalScore;

    // 等级 NORMAL/MILD/MODERATE/SEVERE
    private String level;

    // 风险标记
    private Integer riskFlag;

    // 完成时间
    private LocalDateTime completedAt;

    // 创建时间
    private LocalDateTime createdAt;

    // 量表编码
    private String scaleCode;

    // 量表名称
    private String scaleName;

    // 量表类别
    private String scaleCategory;
}
