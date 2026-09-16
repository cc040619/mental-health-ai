package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程章节保存 DTO（新增/编辑）
 *
 * <p>音频为主，视频可选预留。音频/视频二选一必填其一。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ChapterSaveDTO {

    @NotBlank(message = "章节标题不能为空")
    @Size(max = 200, message = "章节标题不能超过200字")
    private String title;

    @Size(max = 500, message = "音频地址不能超过500字")
    private String audioUrl;

    @Size(max = 500, message = "视频地址不能超过500字")
    private String videoUrl;

    @PositiveOrZero(message = "时长必须为非负整数")
    private Integer duration;

    private String intro;
}
