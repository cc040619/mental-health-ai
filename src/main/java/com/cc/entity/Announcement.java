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
 * 系统公告实体，对应 announcement 表
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("announcement")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // 富文本 HTML，存库前经 HtmlSanitizer 白名单清洗
    private String content;

    // 类型（AnnouncementType 枚举字符串，历史数据默认 SYSTEM）
    @TableField("type")
    private String type;

    // 发布范围：ALL-全体 USERS-指定用户（target_user_ids 非空生效）
    @TableField("target_type")
    private String targetType;

    // 指定用户 ID 数组（JSON 字符串，target_type=USERS 时生效）
    @TableField("target_user_ids")
    private String targetUserIds;

    // 定时发布时间（空=非定时；到点由定时任务置为已发布）
    @TableField("publish_at")
    private LocalDateTime publishAt;

    // 0-草稿 1-已发布 2-已撤回（AnnouncementStatus 枚举；存量旧 0 已迁移为 2）
    private Integer status;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
