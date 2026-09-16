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
 * 个人安全计划实体，对应 crisis_safety_plan 表（user_id 唯一，每用户一行）
 *
 * <p>json 列（loved_ones / coping_strategies）以 String 存储，业务层用 Jackson 序列化/反序列化，
 * 不引入 MyBatis-Plus TypeHandler，保持与现有 json 列处理一致。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("crisis_safety_plan")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrisisSafetyPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 可信赖的人 JSON：[{name, relation, phone}]，原始字符串 */
    @TableField("loved_ones")
    private String lovedOnes;

    /** 应对策略 JSON：字符串数组，原始字符串 */
    @TableField("coping_strategies")
    private String copingStrategies;

    @TableField("emergency_contact")
    private String emergencyContact;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
