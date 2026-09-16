package com.cc.dto.response;

import lombok.Data;

/**
 * 可预约时段 DTO。
 * 时间统一输出 HH:mm 规范化字符串，前端可直接做等值判断与展示，禁止前端二次拼格式。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class SlotResponseDTO {

    private Long id;

    /** yyyy-MM-dd */
    private String slotDate;

    /** HH:mm */
    private String startTime;

    /** HH:mm */
    private String endTime;

    /** 0-可约 1-已约 2-停用 */
    private Integer status;

    /** 已约时的来访者昵称（咨询师端日程展示；其他场景为 null，不含手机号/邮箱） */
    private String userNickname;
}
