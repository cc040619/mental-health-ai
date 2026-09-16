package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员操作日志实体（AOP 异步写入，敏感参数已脱敏）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_operation_log")
public class AdminOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorName;

    /** CREATE / UPDATE / DELETE / LOGIN / LOGOUT */
    private String actionType;

    /** 方法名+参数摘要（敏感字段已替换 ***，截断 1000 字） */
    private String actionContent;

    private String ip;

    private LocalDateTime createdAt;
}
