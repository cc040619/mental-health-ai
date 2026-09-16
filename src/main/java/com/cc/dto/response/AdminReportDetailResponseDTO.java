package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 管理端测评报告详情响应 DTO（敏感数据，仅管理员可见）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminReportDetailResponseDTO {

    private Long recordId;
    private Long userId;
    private String userNickname;
    private Long scaleId;
    private String scaleName;
    private BigDecimal totalScore;
    private String level;
    private String levelLabel;
    private Integer riskFlag;
    private List<Map<String, Object>> answers; // 答案明细
    private Map<String, BigDecimal> dimensionScores; // 维度得分（雷达图数据）
    private String interpretation; // 报告解读
    private List<String> suggestions; // 建议
    private Integer interventionStatus;
    private String interventionNote;
}
