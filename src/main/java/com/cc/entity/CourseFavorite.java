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
 * 课程收藏实体，对应数据库 course_favorite 表
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@TableName("course_favorite")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseFavorite {

    // 收藏ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 课程ID
    @TableField("course_id")
    private Long courseId;

    // 收藏时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
