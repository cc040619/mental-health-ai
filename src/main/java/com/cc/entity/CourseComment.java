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
 * 课程评论实体，对应数据库 course_comment 表
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@TableName("course_comment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseComment {

    // 评论ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 课程ID
    @TableField("course_id")
    private Long courseId;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 评论内容
    @TableField("content")
    private String content;

    // 评论时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
