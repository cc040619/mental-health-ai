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
 * AI 回复拦截记录实体（只存内容摘要 ≤200 字，不存全文）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_block_record")
public class AiBlockRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String triggerWord;

    private String contentSummary;

    private String action;

    private LocalDateTime createdAt;
}
