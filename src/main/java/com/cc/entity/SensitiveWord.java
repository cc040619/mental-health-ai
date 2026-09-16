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
 * 敏感词实体，对应 sensitive_word 表（仅违规词；危机词走 classpath 文件不纳入管理端）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("sensitive_word")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    // POLITICAL/VIOLENT/VULGAR/AD/OTHER
    private String category;

    // 0-停用 1-启用
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
