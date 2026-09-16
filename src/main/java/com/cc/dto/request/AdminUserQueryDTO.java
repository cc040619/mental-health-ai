package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理端用户列表查询参数
 *
 * <p>keyword 匹配昵称或用户名（中缀 LIKE），phone 匹配手机号（中缀 LIKE）；
 * status=0 禁用 / 1 正常；startDate/endDate 按注册时间区间筛选（yyyy-MM-dd）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminUserQueryDTO {

    @Min(value = 1, message = "页码最小为1")
    private Long page = 1L;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 20, message = "每页条数最大为20")
    private Long size = 10L;

    // 昵称/用户名关键字（中缀匹配，数据量大时改为前缀匹配+索引）
    private String keyword;

    // 手机号（中缀匹配）
    private String phone;

    // 状态：0 禁用 / 1 正常
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;

    // 注册起始日期 yyyy-MM-dd
    private String startDate;

    // 注册结束日期 yyyy-MM-dd
    private String endDate;
}
