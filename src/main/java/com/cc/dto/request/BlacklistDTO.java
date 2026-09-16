package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 社区黑名单添加请求
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class BlacklistDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Size(max = 200, message = "屏蔽原因不能超过200字")
    private String reason;
}
