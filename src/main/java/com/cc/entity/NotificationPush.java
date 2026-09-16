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
 * 消息推送批次实体，对应 notification_push 表
 *
 * <p>推送 = 批量写 notification（related_type=PUSH、related_id=批次ID）。
 * 送达率口径：站内消息落库即送达，固定 100%，不落库冗余；
 * 阅读数实时 COUNT（is_read=1），批次表只存目标数。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("notification_push")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPush {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 推送类型（NotificationType 枚举字符串）
    private String type;

    private String title;

    private String content;

    // ALL-全体 USERS-指定用户
    @TableField("target_type")
    private String targetType;

    // 目标用户数
    @TableField("target_count")
    private Integer targetCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
