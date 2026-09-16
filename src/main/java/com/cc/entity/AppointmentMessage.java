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
 * 预约留言实体，对应 appointment_message 表。
 * WebSocket 与 REST 兜底共用同一落库逻辑。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("appointment_message")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("sender_id")
    private Long senderId;

    /** 1-用户 2-咨询师/管理员 */
    @TableField("sender_type")
    private Integer senderType;

    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
