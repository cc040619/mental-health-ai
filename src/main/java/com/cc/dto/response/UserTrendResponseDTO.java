package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户增长趋势响应 DTO：新增用户序列 + 累计用户序列（Service 层逐日/逐月累加，保证累计不回落）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTrendResponseDTO {

    // 粒度：day/month
    private String granularity;

    // 日期（yyyy-MM-dd）或月份（yyyy-MM）轴标签
    private List<String> dates;

    // 各时间点新增用户数
    private List<Long> newUsers;

    // 各时间点累计用户数（单调递增）
    private List<Long> cumulativeUsers;
}
