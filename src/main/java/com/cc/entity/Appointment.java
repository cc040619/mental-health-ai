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
import java.time.LocalTime;

/**
 * 咨询预约实体，对应 appointment 表。
 *
 * <p>状态机见 {@link com.cc.enums.AppointmentStatus}；
 * remindedAt 供消息通知模块「提前 1 小时提醒」任务复用。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("appointment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("consultant_id")
    private Long consultantId;

    @TableField("slot_id")
    private Long slotId;

    @TableField("appointment_date")
    private LocalDate appointmentDate;

    @TableField("start_time")
    private LocalTime startTime;

    @TableField("end_time")
    private LocalTime endTime;

    @TableField("problem_desc")
    private String problemDesc;

    /** 取消原因（取消时填写） */
    @TableField("cancel_reason")
    private String cancelReason;

    /** 取消时间 */
    @TableField("canceled_at")
    private LocalDateTime canceledAt;

    /** PENDING/CONFIRMED/COMPLETED/CANCELED */
    private String status;

    @TableField("reminded_at")
    private LocalDateTime remindedAt;

    /** 0-未评价 1-已评价 */
    private Integer evaluated;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
