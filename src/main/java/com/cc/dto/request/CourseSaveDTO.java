package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程基本信息保存 DTO（创建/编辑）
 *
 * <p>状态由服务端按状态机管理，不在此 DTO 暴露；章节课时由独立接口管理。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CourseSaveDTO {

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "课程标题不能超过200字")
    private String title;

    @NotBlank(message = "课程类别不能为空")
    @Pattern(regexp = "MEDITATION|SLEEP|EMOTION|CBT|INTERPERSONAL",
            message = "课程类别必须为 MEDITATION/SLEEP/EMOTION/CBT/INTERPERSONAL 之一")
    private String category;

    @Size(max = 50, message = "讲师姓名不能超过50字")
    private String lecturer;

    @Size(max = 500, message = "封面URL不能超过500字")
    private String coverUrl;

    private String description;

    @Size(max = 200, message = "适合人群不能超过200字")
    private String targetAudience;
}
