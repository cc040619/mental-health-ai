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
 * 可预约时段实体，对应 appointment_slot 表。
 * status：0-可约 1-已约 2-停用。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("appointment_slot")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("consultant_id")
    private Long consultantId;

    @TableField("slot_date")
    private LocalDate slotDate;

    @TableField("start_time")
    private LocalTime startTime;

    @TableField("end_time")
    private LocalTime endTime;

    /** 0-可约 1-已约 2-停用 */
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
