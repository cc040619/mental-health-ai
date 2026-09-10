package com.cc.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 每日练习打卡请求 DTO
 * <p>服务端按当日该章节进度已播 ≥60 秒校验，不接受前端自报完成
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class PracticeCheckinDTO {

    // 课程ID
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    // 章节ID
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    // 练习时长(秒)
    @Min(value = 0, message = "练习时长不能为负")
    private Integer duration;
}
