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
 * 心理课程实体，对应数据库 course 表
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@TableName("course")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    // 课程ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 课程标题
    @TableField("title")
    private String title;

    // 类别：MEDITATION/SLEEP/EMOTION/CBT/INTERPERSONAL
    @TableField("category")
    private String category;

    // 课程介绍
    @TableField("description")
    private String description;

    // 封面图
    @TableField("cover_url")
    private String coverUrl;

    // 总时长(秒)
    @TableField("total_duration")
    private Integer totalDuration;

    // 学习人数
    @TableField("learn_count")
    private Integer learnCount;

    // 状态 1:上架 0:下架
    @TableField("status")
    private Integer status;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
