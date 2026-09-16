package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 咨询师排班批量新增请求（管理员模拟咨询师端排班）。
 * 二选一：{@code date} 按日批量；{@code weekStart} 周模板批量（自动归一到周一，
 * slots 作为「每日模板」应用到周一~周日，已存在时段跳过不重复）。
 *
 * <p>ponytail: 真实咨询师端上线后，本 DTO 与排班接口可直接复用，仅替换调用方前端。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class SlotBatchCreateDTO {

    /** 按日批量日期（与 weekStart 二选一） */
    private LocalDate date;

    /** 周模板批量：传周内任意一天即可，后端归一到当周周一（与 date 二选一） */
    private LocalDate weekStart;

    @Valid
    @NotNull(message = "请至少添加一个时段")
    private List<SlotTime> slots;

    /**
     * 单个时段的起止时间
     */
    @Data
    public static class SlotTime {

        @NotNull(message = "开始时间不能为空")
        private LocalTime startTime;

        @NotNull(message = "结束时间不能为空")
        private LocalTime endTime;
    }
}
