package com.cc.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 实时测试 DTO（无记忆单轮调用，真实消耗 token）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiTestDTO {

    @Size(max = 1000, message = "测试消息长度不能超过1000")
    private String message;
}
