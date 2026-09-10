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
 * 章节学习进度实体，对应数据库 course_progress 表
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@TableName("course_progress")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgress {

    // 进度ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 章节ID
    @TableField("chapter_id")
    private Long chapterId;

    // 已播秒数
    @TableField("progress_seconds")
    private Integer progressSeconds;

    // 章节时长(秒)
    @TableField("duration")
    private Integer duration;

    // 是否已完成 1:是 0:否
    @TableField("is_finished")
    private Integer isFinished;

    // 最近播放时间
    @TableField("last_played_at")
    private LocalDateTime lastPlayedAt;
}
