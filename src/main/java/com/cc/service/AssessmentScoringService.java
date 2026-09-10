package com.cc.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cc.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 心理测评计分引擎（无状态纯计算类）
 * <p>
 * 输入：作答答案 + scoring_config JSON，输出：总分、维度得分、等级、风险标记。
 * 不依赖数据库与业务状态，便于单元测试。
 *
 * 评分规则：
 * 1. 普通题：score 直接累加；
 * 2. 反向题（reverseScoreQuestionIndexes）：score = reverseMaxScore - score；
 * 3. 维度得分（dimensions）：sum 该维度题目计分后的分值，可选 scoreMap 映射为 0-3；
 * 4. 总分：totalFromDimensions=true 时为各维度得分之和，否则为各题计分后分值之和；
 * 5. 等级：按 levels 区间闭区间 [min, max] 判定；
 * 6. 风险：riskQuestionIndexes 中任一题得分 &ge; riskScoreThreshold，或等级为 SEVERE &rarr; riskFlag=1。
 *
 * @author cc
 * @date 2026-09-08
 */
@Slf4j
@Service
public class AssessmentScoringService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 计算测评结果
     *
     * @param scoringConfigJson 量表计分配置 JSON 字符串
     * @param answers           作答答案（questionIndex -> score）
     * @return 计分结果
     */
    public ScoringResult calculate(String scoringConfigJson, Map<Integer, Integer> answers) {
        ScoringConfig config = parseConfig(scoringConfigJson);
        return calculate(config, answers);
    }

    /**
     * 计算测评结果（直接传入解析后的配置）
     *
     * @param config  计分配置
     * @param answers 作答答案（questionIndex -> score）
     * @return 计分结果
     */
    public ScoringResult calculate(ScoringConfig config, Map<Integer, Integer> answers) {
        if (config == null || config.getQuestions() == null || config.getQuestions().isEmpty()) {
            throw new BusinessException("量表计分配置无效");
        }
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException("作答答案不能为空");
        }

        // MBTI 极性计分模式（二选一人格类型）
        if ("polarity".equalsIgnoreCase(config.getScoringMode())) {
            return calculatePolarity(config, answers);
        }

        return calculateSum(config, answers);
    }

    /**
     * 标准加和计分模式（PHQ-9 / GAD-7 / PSS-10 / PSQI / SCL-90 等）
     */
    private ScoringResult calculateSum(ScoringConfig config, Map<Integer, Integer> answers) {

        // 反向题索引集合
        Set<Integer> reverseIndexes = config.getReverseScoreQuestionIndexes() == null
                ? Collections.emptySet() : new HashSet<>(config.getReverseScoreQuestionIndexes());
        int reverseMax = config.getReverseMaxScore() == null ? 0 : config.getReverseMaxScore();

        // 计算每题最终分值（应用反向计分）
        Map<Integer, BigDecimal> questionScores = new LinkedHashMap<>();
        for (ScoringQuestion q : config.getQuestions()) {
            Integer idx = q.getIndex();
            Integer raw = answers.get(idx);
            if (raw == null) {
                throw new BusinessException("第 " + (idx + 1) + " 题未作答");
            }
            BigDecimal finalScore;
            if (reverseIndexes.contains(idx) && reverseMax > 0) {
                finalScore = BigDecimal.valueOf(reverseMax - raw);
            } else {
                finalScore = BigDecimal.valueOf(raw);
            }
            questionScores.put(idx, finalScore);
        }

        // 维度得分
        Map<String, BigDecimal> dimensionScores = new LinkedHashMap<>();
        if (config.getDimensions() != null) {
            for (ScoringDimension dim : config.getDimensions()) {
                BigDecimal dimSum = BigDecimal.ZERO;
                if (dim.getQuestionIndexes() != null) {
                    for (Integer qIdx : dim.getQuestionIndexes()) {
                        BigDecimal qs = questionScores.get(qIdx);
                        if (qs != null) {
                            dimSum = dimSum.add(qs);
                        }
                    }
                }
                // 应用 scoreMap（将原始分和映射为目标区间，如 PSQI 各成分 0-3）
                BigDecimal dimScore = applyScoreMap(dimSum, dim.getScoreMap());
                dimensionScores.put(dim.getName(), dimScore);
            }
        }

        // 总分
        BigDecimal totalScore;
        if (Boolean.TRUE.equals(config.getTotalFromDimensions()) && !dimensionScores.isEmpty()) {
            totalScore = BigDecimal.ZERO;
            for (BigDecimal ds : dimensionScores.values()) {
                totalScore = totalScore.add(ds);
            }
        } else {
            totalScore = BigDecimal.ZERO;
            for (BigDecimal qs : questionScores.values()) {
                totalScore = totalScore.add(qs);
            }
        }
        totalScore = totalScore.setScale(2, RoundingMode.HALF_UP);

        // 等级判定
        ScoringLevel matched = matchLevel(config.getLevels(), totalScore);
        String level = matched != null ? matched.getLevel() : "NORMAL";

        // 风险判定
        boolean risk = isRisk(config, answers, level);
        int riskFlag = risk ? 1 : 0;

        return new ScoringResult(totalScore, dimensionScores, level, riskFlag, config);
    }

    /**
     * MBTI 极性计分模式：每题二选一，按极性分组计数，比较 E/I、S/N、T/F、J/P 取多数
     * <p>
     * 每个 option 可带 {@code polarity} 字段（如 "E"、"I"、"S"、"N"），
     * 同一维度下极性字母相同的选项归为一组，统计各组选择次数。
     * 维度结果 = 次数较多的极性字母。
     * 最终人格类型 = 4 维度极性字母拼接（如 "INTJ"、"ENFP"）。
     */
    private ScoringResult calculatePolarity(ScoringConfig config, Map<Integer, Integer> answers) {
        // 1. 先按 questionIndex 找到用户选择的 option，提取其 polarity
        Map<Integer, String> questionPolarities = new LinkedHashMap<>();
        for (ScoringQuestion q : config.getQuestions()) {
            Integer idx = q.getIndex();
            Integer chosenScore = answers.get(idx);
            if (chosenScore == null) {
                throw new BusinessException("第 " + (idx + 1) + " 题未作答");
            }
            String polarity = null;
            if (q.getOptions() != null) {
                for (ScoringOption opt : q.getOptions()) {
                    if (opt.getScore() != null && opt.getScore().equals(chosenScore)) {
                        polarity = opt.getPolarity();
                        break;
                    }
                }
            }
            if (polarity == null || polarity.isBlank()) {
                throw new BusinessException("第 " + (idx + 1) + " 题的选项缺少 polarity 配置");
            }
            questionPolarities.put(idx, polarity);
        }

        // 2. 统计各维度的极性选择次数
        Map<String, BigDecimal> dimensionScores = new LinkedHashMap<>();
        StringBuilder typeBuilder = new StringBuilder();
        if (config.getDimensions() != null) {
            for (ScoringDimension dim : config.getDimensions()) {
                Map<String, Long> polarityCount = new LinkedHashMap<>();
                if (dim.getQuestionIndexes() != null) {
                    for (Integer qIdx : dim.getQuestionIndexes()) {
                        String pol = questionPolarities.get(qIdx);
                        if (pol != null) {
                            polarityCount.merge(pol, 1L, Long::sum);
                        }
                    }
                }
                // 取次数较多的极性
                String winner = null;
                long maxCount = 0;
                for (Map.Entry<String, Long> e : polarityCount.entrySet()) {
                    if (e.getValue() > maxCount) {
                        maxCount = e.getValue();
                        winner = e.getKey();
                    }
                }
                // 维度得分 = 多数派得票占比（0-100）
                long totalInDim = polarityCount.values().stream().mapToLong(Long::longValue).sum();
                BigDecimal dimScore = totalInDim > 0
                        ? BigDecimal.valueOf(maxCount * 100.0 / totalInDim).setScale(1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                dimensionScores.put(dim.getName() + "(" + (winner != null ? winner : "?") + ")", dimScore);

                if (winner != null && winner.length() >= 1) {
                    typeBuilder.append(winner.charAt(0));
                }
            }
        }

        // 3. 构造 MBTI 人格类型字符串作为 level（如 "INTJ"），totalScore 用 0 占位
        String typeResult = typeBuilder.toString();
        String level = typeResult.isBlank() ? "UNKNOWN" : typeResult;
        BigDecimal totalScore = BigDecimal.ZERO;
        int riskFlag = 0; // MBTI 无风险判定

        ScoringResult result = new ScoringResult(totalScore, dimensionScores, level, riskFlag, config);
        result.setTypeResult(typeResult);
        return result;
    }

    /**
     * 应用 scoreMap，将原始分和映射为目标分（区间上界 max，score 为映射分值）
     */
    private BigDecimal applyScoreMap(BigDecimal rawSum, List<ScoreMapEntry> scoreMap) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return rawSum.setScale(2, RoundingMode.HALF_UP);
        }
        for (ScoreMapEntry entry : scoreMap) {
            if (rawSum.compareTo(BigDecimal.valueOf(entry.getMax())) <= 0) {
                return BigDecimal.valueOf(entry.getScore()).setScale(2, RoundingMode.HALF_UP);
            }
        }
        // 超过所有上界，取最后一条的 score
        return BigDecimal.valueOf(scoreMap.get(scoreMap.size() - 1).getScore()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 等级匹配（闭区间 [min, max]）
     */
    private ScoringLevel matchLevel(List<ScoringLevel> levels, BigDecimal totalScore) {
        if (levels == null || levels.isEmpty()) {
            return null;
        }
        for (ScoringLevel lv : levels) {
            if (totalScore.compareTo(BigDecimal.valueOf(lv.getMin())) >= 0
                    && totalScore.compareTo(BigDecimal.valueOf(lv.getMax())) <= 0) {
                return lv;
            }
        }
        return levels.get(levels.size() - 1);
    }

    /**
     * 风险判定：风险题得分达阈值 或 等级为 SEVERE
     */
    private boolean isRisk(ScoringConfig config, Map<Integer, Integer> answers, String level) {
        if ("SEVERE".equals(level)) {
            return true;
        }
        if (config.getRiskQuestionIndexes() == null || config.getRiskQuestionIndexes().isEmpty()) {
            return false;
        }
        int threshold = config.getRiskScoreThreshold() == null ? 0 : config.getRiskScoreThreshold();
        for (Integer idx : config.getRiskQuestionIndexes()) {
            Integer s = answers.get(idx);
            if (s != null && s >= threshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 scoring_config JSON
     */
    public ScoringConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException("量表计分配置为空");
        }
        try {
            return MAPPER.readValue(json, ScoringConfig.class);
        } catch (Exception e) {
            log.error("解析量表计分配置失败", e);
            throw new BusinessException("量表计分配置格式错误");
        }
    }

    // ==================== 计分配置模型（对应 scoring_config JSON） ====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringConfig {
        /** 计分模式："sum"（默认，加和计分）/ "polarity"（MBTI 二选一极性计分） */
        private String scoringMode;
        private List<ScoringQuestion> questions;
        private List<Integer> reverseScoreQuestionIndexes;
        private Integer reverseMaxScore;
        private Boolean totalFromDimensions;
        private List<ScoringDimension> dimensions;
        private List<ScoringLevel> levels;
        private List<Integer> riskQuestionIndexes;
        private Integer riskScoreThreshold;
        private List<String> categoryAdvice;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringQuestion {
        private Integer index;
        private String text;
        private List<ScoringOption> options;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringOption {
        private String label;
        private Integer score;
        /** 极性标记（MBTI 模式下使用，如 "E"、"I"、"S"、"N"） */
        private String polarity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringDimension {
        private String name;
        private List<Integer> questionIndexes;
        private List<ScoreMapEntry> scoreMap;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreMapEntry {
        private int max;
        private int score;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringLevel {
        private String level;
        private int min;
        private int max;
        private String description;
        private String suggestion;
    }

    // ==================== 计分结果 ====================

    @Data
    public static class ScoringResult {
        private final BigDecimal totalScore;
        private final Map<String, BigDecimal> dimensionScores;
        private final String level;
        private final int riskFlag;
        private final ScoringConfig config;
        /** MBTI 极性模式下的人格类型字符串（如 "INTJ"），sum 模式下为 null */
        private String typeResult;

        /**
         * 获取已匹配的等级配置（用于报告解读与建议）
         */
        public ScoringLevel getMatchedLevel() {
            if (config == null || config.getLevels() == null) {
                return null;
            }
            for (ScoringLevel lv : config.getLevels()) {
                if (lv.getLevel().equals(level)) {
                    return lv;
                }
            }
            return null;
        }

        /**
         * 获取类别通用建议
         */
        public List<String> getCategoryAdvice() {
            if (config != null && config.getCategoryAdvice() != null) {
                return new ArrayList<>(config.getCategoryAdvice());
            }
            return Collections.emptyList();
        }
    }
}
