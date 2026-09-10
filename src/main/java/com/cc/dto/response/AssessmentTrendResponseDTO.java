package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测评历史趋势响应 DTO（同量表多次测评折线图）
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentTrendResponseDTO {

    // 记录ID
    private Long id;

    // 总分
    private BigDecimal totalScore;

    // 等级
    private String level;

    // 完成时间
    private LocalDateTime completedAt;
}
