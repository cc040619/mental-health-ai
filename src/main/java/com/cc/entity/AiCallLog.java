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
 * AI 调用日志实体（AOP 埋点异步写入）
 *
 * <p>口径：调用次数/耗时/成功率精确；token 尽力从响应 usage 提取，无则 NULL，
 * 流式响应 usage 难取（毕设口径），升级路径=ChatModel 观测点或网关计费。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String purpose;

    private String model;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer durationMs;

    private Integer success;

    private String errorMsg;

    private LocalDateTime createdAt;
}
