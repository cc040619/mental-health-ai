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
 * 公告已读实体，对应 announcement_read 表（announcement_id + user_id 唯一）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("announcement_read")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("announcement_id")
    private Long announcementId;

    @TableField("user_id")
    private Long userId;

    @TableField("read_at")
    private LocalDateTime readAt;
}
