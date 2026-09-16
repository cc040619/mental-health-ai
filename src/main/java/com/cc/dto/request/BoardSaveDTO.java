package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 板块新增/编辑请求（编辑时 id 必传）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class BoardSaveDTO {

    // 编辑时必传，新增为 null
    private Long id;

    @NotBlank(message = "板块编码不能为空")
    @Pattern(regexp = "^[A-Z0-9_]{2,30}$", message = "板块编码为大写字母/数字/下划线，2-30位")
    private String code;

    @NotBlank(message = "板块名称不能为空")
    @Size(max = 50, message = "板块名称不能超过50字")
    private String name;

    @Size(max = 200, message = "图标地址不能超过200字")
    private String icon;

    @Size(max = 200, message = "板块描述不能超过200字")
    private String description;

    private Integer sortOrder;

    // 版主用户ID（可选）
    private Long moderatorId;

    // 0-停用 1-启用
    private Integer status;
}
