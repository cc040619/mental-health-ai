package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测评作答记录实体，对应数据库 assessment_record 表
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
@TableName("assessment_record")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRecord {

    // 记录ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 量表ID
    @TableField("scale_id")
    private Long scaleId;

    // 状态：IN_PROGRESS-进行中/COMPLETED-已完成
    @TableField("status")
    private String status;

    // 当前答到第几题(从0开始)
    @TableField("current_index")
    private Integer currentIndex;

    // 答案数组 JSON字符串 [{"questionIndex":0,"score":2}]
    @TableField("answers")
    private String answers;

    // 总分
    @TableField("total_score")
    private BigDecimal totalScore;

    // 等级 NORMAL/MILD/MODERATE/SEVERE
    @TableField("level")
    private String level;

    // 维度得分 JSON字符串 {"维度名":得分}
    @TableField("dimension_scores")
    private String dimensionScores;

    // 报告内容 JSON字符串（解读/建议/风险提示）
    @TableField("report")
    private String report;

    // 风险标记 1-需关注
    @TableField("risk_flag")
    private Integer riskFlag;

    // 干预状态 0-未处理 1-已干预 2-已处理
    @TableField("intervention_status")
    private Integer interventionStatus;

    // 干预备注
    @TableField("intervention_note")
    private String interventionNote;

    // 干预时间
    @TableField("intervened_at")
    private LocalDateTime intervenedAt;

    // 开始作答时间
    @TableField("started_at")
    private LocalDateTime startedAt;

    // 完成时间
    @TableField("completed_at")
    private LocalDateTime completedAt;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
