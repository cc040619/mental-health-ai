package com.cc.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 章节学习进度上报请求 DTO
 * <p>前端节流 10 秒上报一次；服务端按唯一键(user+chapter) upsert
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseProgressUpdateDTO {

    // 章节ID
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    // 已播秒数
    @NotNull(message = "播放进度不能为空")
    @Min(value = 0, message = "播放进度不能为负")
    private Integer progressSeconds;

    // 章节时长(秒，由前端上报，服务端以 DB 章节时长为准校验)
    @Min(value = 0, message = "章节时长不能为负")
    private Integer duration;
}
