package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 消息推送请求 DTO（批量写站内通知 + 批次记录）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class NotificationPushDTO {

    // 推送类型：复用通知类型枚举（Service 层校验）
    @NotBlank(message = "推送类型不能为空")
    private String type;

    @NotBlank(message = "推送标题不能为空")
    @Size(max = 200, message = "推送标题不能超过200个字符")
    private String title;

    @NotBlank(message = "推送内容不能为空")
    @Size(max = 1000, message = "推送内容不能超过1000个字符")
    private String content;

    // 发布范围：ALL-全体 USERS-指定用户（默认 ALL）
    private String targetType;

    // 指定用户 ID 列表（targetType=USERS 时必填，≤500/次）
    @Size(max = 500, message = "单次指定用户推送不能超过500人")
    private List<Long> targetUserIds;
}
