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
 * 危机事件记录实体，对应 crisis_event 表
 *
 * <p><b>隐私保护</b>：仅记录元数据（user_id / source / matched_keyword / created_at），
 * 禁止存储对话内容、用户消息、AI 回复，体现可追踪性同时保护用户隐私。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("crisis_event")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrisisEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 触发来源：CHAT对话 / COMMUNITY社区 / ASSESSMENT测评 */
    @TableField("source")
    private String source;

    /** 命中的危机词（仅记录词本身，不含上下文） */
    @TableField("matched_keyword")
    private String matchedKeyword;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
