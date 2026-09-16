package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 量表题目整体保存 DTO
 * <p>题目 + 维度 + 指导语 + 解释模板整体提交，序列化进 scoring_config。
 * 不逐题增删改接口，减少一致性风险。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ScaleQuestionsDTO {

    @NotEmpty(message = "题目不能为空")
    @Valid
    private List<QuestionItem> questions;

    @Valid
    private List<DimensionItem> dimensions;

    @Size(max = 1000, message = "指导语不能超过1000字")
    private String guidance;

    // 结果解释模板：{NORMAL:文本, MILD:..., MODERATE:..., SEVERE:...}
    private Map<String, @Size(max = 2000, message = "解释模板单条不能超过2000字") String> interpretationTemplates;

    @Data
    public static class QuestionItem {
        private Integer index;
        @NotBlank(message = "题干不能为空")
        @Size(max = 500, message = "题干不能超过500字")
        private String text;
        @NotEmpty(message = "选项不能为空")
        @Valid
        private List<OptionItem> options;
    }

    @Data
    public static class OptionItem {
        private String label;
        @NotNull(message = "选项分值不能为空")
        private Integer score;
        private String polarity; // MBTI 极性模式可选
    }

    @Data
    public static class DimensionItem {
        private String name;
        private List<Integer> questionIndexes;
        private List<ScoreMapEntry> scoreMap;
    }

    @Data
    public static class ScoreMapEntry {
        private Integer max;
        private Integer score;
    }
}
