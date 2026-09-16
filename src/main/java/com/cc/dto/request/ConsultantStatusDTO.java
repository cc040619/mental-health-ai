package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 咨询师禁用/恢复请求 DTO，对应前端 body { status: 1|2 }。
 * 1-正常(恢复) 2-禁用；待审核(0)记录不可走该接口（先审核）。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class ConsultantStatusDTO {

    /** 1-正常 2-禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
