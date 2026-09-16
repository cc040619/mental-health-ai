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
 * 咨询记录实体，对应 consultation_record 表（一单一记录）。
 *
 * <p>可见性：咨询师本人读写 + 管理员只读；用户端无任何接口暴露。
 * 业务键 appointment_id 唯一，按 user_id 查询构成「该来访者历史咨询记录」。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
@TableName("consultation_record")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("user_id")
    private Long userId;

    @TableField("consultant_id")
    private Long consultantId;

    /** 主诉 */
    @TableField("chief_complaint")
    private String chiefComplaint;

    /** 分析 */
    private String analysis;

    /** 干预方案 */
    @TableField("intervention_plan")
    private String interventionPlan;

    /** 下次建议 */
    @TableField("next_suggestion")
    private String nextSuggestion;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
