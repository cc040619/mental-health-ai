package com.cc.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告创建 / 编辑请求 DTO（三态版）
 *
 * <p>创建一律存草稿（DRAFT），由 /publish 立即发布或 publishAt 到点定时发布；
 * 编辑仅限 DRAFT/WITHDRAWN（已发布须先撤回，内容安全约束）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AnnouncementCreateDTO {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200个字符")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    // 类型：SYSTEM/ACTIVITY/TIP（Service 层枚举白名单校验）
    private String type;

    // 发布范围：ALL-全体 USERS-指定用户（默认 ALL）
    private String targetType;

    // 指定用户 ID 列表（targetType=USERS 时必填）
    @Size(max = 500, message = "指定用户不能超过500个")
    private List<Long> targetUserIds;

    // 定时发布时间（空=非定时；必须为未来时间）
    @Future(message = "定时发布时间必须晚于当前时间")
    private LocalDateTime publishAt;
}
