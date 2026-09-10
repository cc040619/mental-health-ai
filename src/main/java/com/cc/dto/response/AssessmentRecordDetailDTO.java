package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 测评记录详情响应 DTO（GET /records/{id} 统一返回）
 * <ul>
 *   <li>作答页（IN_PROGRESS）：读取 status/currentIndex/answers/scale 信息</li>
 *   <li>报告页（COMPLETED）：读取 totalScore/level/dimensionScores/解读/建议/风险/免责声明</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentRecordDetailDTO {

    // 记录ID
    private Long recordId;

    // 量表ID
    private Long scaleId;

    // 状态 IN_PROGRESS/COMPLETED
    private String status;

    // 当前题序（IN_PROGRESS 续答用）
    private Integer currentIndex;

    // 已作答答案（questionIndex + score，IN_PROGRESS 续答用）
    private List<Map<String, Object>> answers;

    // ===== 量表信息 =====
    private String scaleCode;
    private String scaleName;
    private String scaleCategory;
    private String scaleDescription;

    // ===== 报告字段（COMPLETED 时填充） =====
    private BigDecimal totalScore;
    private String level;
    private String levelLabel;
    private String levelColor;
    private Map<String, BigDecimal> dimensionScores;
    private String interpretation;
    private List<String> suggestions;
    private Integer riskFlag;
    private String riskMessage;
    private String disclaimer;
    private LocalDateTime completedAt;
}
