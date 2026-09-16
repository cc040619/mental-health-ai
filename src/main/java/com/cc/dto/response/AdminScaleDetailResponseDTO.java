package com.cc.dto.response;

import com.cc.service.AssessmentScoringService;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端量表详情响应 DTO
 * <p>解析 scoring_config，返回题目数组 + 维度配置 + 指导语 + 解释模板</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminScaleDetailResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String category;
    private String description;
    private Integer questionCount;
    private Integer estimatedMinutes;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String guidance;
    private Map<String, String> interpretationTemplates;

    private List<AssessmentScoringService.ScoringQuestion> questions;
    private List<AssessmentScoringService.ScoringDimension> dimensions;
    private List<Integer> reverseScoreQuestionIndexes;
    private Integer reverseMaxScore;
    private Boolean totalFromDimensions;
    private List<AssessmentScoringService.ScoringLevel> levels;
    private List<Integer> riskQuestionIndexes;
    private Integer riskScoreThreshold;
}
