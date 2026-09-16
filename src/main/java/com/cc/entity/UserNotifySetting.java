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
 * 用户消息设置实体，对应 user_notify_setting 表（user_id 唯一）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("user_notify_setting")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotifySetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("announcement_on")
    private Integer announcementOn;

    @TableField("appointment_on")
    private Integer appointmentOn;

    @TableField("interaction_on")
    private Integer interactionOn;

    @TableField("assessment_on")
    private Integer assessmentOn;

    // NONE/WEEKLY/DAILY
    @TableField("assessment_frequency")
    private String assessmentFrequency;

    @TableField("last_assessment_remind_at")
    private LocalDateTime lastAssessmentRemindAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
