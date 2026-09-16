package com.cc.dto.response;

import lombok.Data;

/**
 * 消息设置响应 DTO
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class NotifySettingResponseDTO {

    private Integer announcementOn;

    private Integer appointmentOn;

    private Integer interactionOn;

    private Integer assessmentOn;

    // NONE/WEEKLY/DAILY
    private String assessmentFrequency;
}
