package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 测评数据分析响应 DTO：各量表测评次数 + 结果等级分布 + 高危用户预警 TOP10
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentStatsResponseDTO {

    // 各量表完成次数（按量表名分组）
    private List<NameCountDTO> scaleCounts;

    // 结果等级分布（固定四档 NORMAL/MILD/MODERATE/SEVERE，缺失补零）
    private List<LevelCount> levelDistribution;

    // 高危用户预警（按最近测评时间倒序前 10，昵称/手机号已脱敏，不含任何作答内容）
    private List<RiskUserItem> riskUsers;

    /**
     * 等级分布项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelCount {
        // 等级编码 NORMAL/MILD/MODERATE/SEVERE
        private String level;
        // 中文标签 正常/轻度/中度/重度
        private String label;
        // 数量
        private Long value;
    }

    /**
     * 高危用户预警项（仅元数据，禁止携带日记/对话/报告内容）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskUserItem {
        // 昵称（匿名为空时回退用户名）
        private String nickname;
        // 脱敏手机号 138****1234
        private String phone;
        // 风险类型：重度测评预警 / 高危风险标记
        private String riskType;
        // 该用户范围内最高等级编码（可能为空字符串）
        private String riskLevel;
        // 最近一次高危测评时间 yyyy-MM-dd HH:mm
        private String lastTime;
        // 范围内高危记录次数
        private Long count;
    }
}
