package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情绪大数据响应 DTO：全站主导情绪分布 + 近 30 天情绪趋势热力图
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionStatsResponseDTO {

    // 范围内主导情绪分布
    private List<NameCountDTO> emotionDistribution;

    // 近30天 日期×情绪 热力图矩阵
    private Heatmap heatmap;

    /**
     * 热力图：data 中每项为 [日期索引, 情绪索引, 记录数]
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Heatmap {
        // X 轴：近30天日期 yyyy-MM-dd（升序）
        private List<String> dates;
        // Y 轴：情绪标签
        private List<String> emotions;
        // 矩阵数据 [xIndex, yIndex, count]
        private List<int[]> data;
    }
}
