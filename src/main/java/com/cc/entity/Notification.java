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
 * 消息通知实体，对应 notification 表
 *
 * <p>系统公告不落本表（走 announcement + announcement_read，避免全量广播爆表）。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("notification")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    // ANNOUNCEMENT/APPOINTMENT/INTERACTION/ASSESSMENT
    private String type;

    private String title;

    private String content;

    // POST/APPOINTMENT/ASSESSMENT
    @TableField("related_type")
    private String relatedType;

    @TableField("related_id")
    private Long relatedId;

    // 0-未读 1-已读
    @TableField("is_read")
    private Integer isRead;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
