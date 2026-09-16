package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 帖子置顶/加精请求（value: 0-取消 1-设置）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class PostFlagDTO {

    @NotNull(message = "取值不能为空")
    private Integer value;
}
