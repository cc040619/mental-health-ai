package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 看板通用名称-数值项，直接适配 ECharts 饼图/柱状图 {name, value} 结构
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameCountDTO {

    // 名称（场景/情绪/量表/等级/标题等）
    private String name;

    // 数值
    private Long value;
}
