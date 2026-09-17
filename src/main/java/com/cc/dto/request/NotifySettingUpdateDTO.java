package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 消息设置更新请求 DTO
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class NotifySettingUpdateDTO {

    @NotNull(message = "公告通知开关不能为空")
    @Min(value = 0, message = "开关值非法")
    @Max(value = 1, message = "开关值非法")
    private Integer announcementOn;

    @NotNull(message = "预约通知开关不能为空")
    @Min(value = 0, message = "开关值非法")
    @Max(value = 1, message = "开关值非法")
    private Integer appointmentOn;

    @NotNull(message = "互动通知开关不能为空")
    @Min(value = 0, message = "开关值非法")
    @Max(value = 1, message = "开关值非法")
    private Integer interactionOn;

    @NotNull(message = "测评提醒开关不能为空")
    @Min(value = 0, message = "开关值非法")
    @Max(value = 1, message = "开关值非法")
    private Integer assessmentOn;

    @NotNull(message = "咨询师回复开关不能为空")
    @Min(value = 0, message = "开关值非法")
    @Max(value = 1, message = "开关值非法")
    private Integer consultantOn;

    @NotBlank(message = "测评提醒频率不能为空")
    @Pattern(regexp = "NONE|WEEKLY|DAILY", message = "测评提醒频率仅支持 NONE/WEEKLY/DAILY")
    private String assessmentFrequency;
}
