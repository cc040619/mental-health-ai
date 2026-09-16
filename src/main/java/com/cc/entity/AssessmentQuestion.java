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
 * 通用题目库实体（素材池），量表编辑时以快照引用
 * <p>题库题修改/删除不影响已建量表（快照隔离）</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("assessment_question")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 题干
    @TableField("stem")
    private String stem;

    // 题型：SINGLE-单选 MULTIPLE-多选 SCALE-量表题
    @TableField("type")
    private String type;

    // 选项 JSON：[{label,text,score}]
    @TableField("options")
    private String options;

    // 默认维度标签（可选，添加到量表时可被覆盖）
    @TableField("dimension_tag")
    private String dimensionTag;

    // 题目库分类
    @TableField("category")
    private String category;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
