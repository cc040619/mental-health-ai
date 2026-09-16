package com.cc.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 咨询师详情 DTO（含个人简介）
 *
 * @author cc
 * @date 2026-09-14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConsultantDetailResponseDTO extends ConsultantListResponseDTO {

    private String intro;

    /** 可预约天数：用户端预约页只渲染/可约今天起未来 N 天（含今天） */
    private Integer bookingWindowDays;
}
