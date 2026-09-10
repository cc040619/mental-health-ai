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
 * 课程章节实体，对应数据库 course_chapter 表
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@TableName("course_chapter")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseChapter {

    // 章节ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 课程ID
    @TableField("course_id")
    private Long courseId;

    // 章节标题
    @TableField("title")
    private String title;

    // 音频地址(静态文件或OSS)
    @TableField("audio_url")
    private String audioUrl;

    // 时长(秒)
    @TableField("duration")
    private Integer duration;

    // 章节图文介绍
    @TableField("intro")
    private String intro;

    // 排序
    @TableField("sort_order")
    private Integer sortOrder;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
