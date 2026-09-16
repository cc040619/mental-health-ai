package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 会话标记请求 DTO（后台管理端，幂等：重复提交覆盖 note）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class SessionMarkDTO {

    // 高质量标记（AI 训练参考）
    @NotNull(message = "高质量标记不能为空")
    private Integer isHighQuality;

    // 危机会话标记（运营关注，与用户端自动危机识别解耦）
    @NotNull(message = "危机标记不能为空")
    private Integer isCrisis;

    // 标记备注
    @Size(max = 500, message = "标记备注不能超过500个字符")
    private String note;
}
