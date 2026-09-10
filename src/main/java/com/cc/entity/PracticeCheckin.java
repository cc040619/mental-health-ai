package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日练习打卡实体，对应数据库 practice_checkin 表
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@TableName("practice_checkin")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeCheckin {

    // 打卡ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 打卡日期
    @TableField("checkin_date")
    private LocalDate checkinDate;

    // 课程ID
    @TableField("course_id")
    private Long courseId;

    // 章节ID
    @TableField("chapter_id")
    private Long chapterId;

    // 练习时长(秒)
    @TableField("duration")
    private Integer duration;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
