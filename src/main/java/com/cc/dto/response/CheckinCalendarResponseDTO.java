package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;

/**
 * 打卡日历单条记录 DTO
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CheckinCalendarResponseDTO {

    // 打卡日期
    private LocalDate checkinDate;

    // 课程标题
    private String courseTitle;

    // 章节标题
    private String chapterTitle;
}
