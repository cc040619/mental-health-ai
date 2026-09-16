package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预约留言发送请求（HTTP 兜底，WebSocket 消息同样受 1000 字长度限制）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class AppointmentMessageDTO {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 1000, message = "留言内容不能超过1000字")
    private String content;
}
