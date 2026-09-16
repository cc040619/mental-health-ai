package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 咨询师回复评价请求（单级回复，重复回复覆盖）。
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class ConsultantReviewReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过500字")
    private String content;
}
