package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 心理量表定义实体，对应数据库 assessment_scale 表
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
@TableName("assessment_scale")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentScale {

    // 量表ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 量表编码（PHQ-9/GAD-7/PSS-10/PSQI）
    @TableField("code")
    private String code;

    // 量表名称
    @TableField("name")
    private String name;

    // 类别：EMOTION/PERSONALITY/STRESS/SLEEP/INTERPERSONAL
    @TableField("category")
    private String category;

    // 量表简介
    @TableField("description")
    private String description;

    // 题目数
    @TableField("question_count")
    private Integer questionCount;

    // 预计时长(分钟)
    @TableField("estimated_minutes")
    private Integer estimatedMinutes;

    // 计分配置JSON字符串（由 Service 层解析）
    @TableField("scoring_config")
    private String scoringConfig;

    // 是否启用 1:启用 0:停用
    @TableField("enabled")
    private Integer enabled;

    // 排序
    @TableField("sort_order")
    private Integer sortOrder;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
