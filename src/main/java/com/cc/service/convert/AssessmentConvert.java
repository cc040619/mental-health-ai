package com.cc.service.convert;

import com.cc.dto.request.AssessmentAnswerItemDTO;
import com.cc.dto.response.AssessmentRecordDetailDTO;
import com.cc.dto.response.AssessmentScaleDetailDTO;
import com.cc.dto.response.AssessmentScaleResponseDTO;
import com.cc.entity.AssessmentRecord;
import com.cc.entity.AssessmentScale;
import com.cc.enums.AssessmentLevel;
import com.cc.exception.BusinessException;
import com.cc.service.AssessmentScoringService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 测评模块对象转换器：Entity/DTO 互转与 JSON 解析
 * 禁止在 controller/service 手写转换，统一走此转换器
 *
 * @author cc
 * @date 2026-09-08
 */
@Slf4j
@Component
public class AssessmentConvert {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DISCLAIMER = "本测评结果由标准化量表自动计算生成，仅供参考，不构成医疗诊断。如有持续困扰，请寻求专业心理帮助";

    private final AssessmentScoringService scoringService;

    public AssessmentConvert(AssessmentScoringService scoringService) {
        this.scoringService = scoringService;
    }

    /**
     * 量表 Entity → 列表项 DTO
     */
    public AssessmentScaleResponseDTO toScaleResponseDTO(AssessmentScale scale) {
        if (scale == null) {
            return null;
        }
        AssessmentScaleResponseDTO dto = new AssessmentScaleResponseDTO();
        dto.setId(scale.getId());
        dto.setCode(scale.getCode());
        dto.setName(scale.getName());
        dto.setCategory(scale.getCategory());
        dto.setDescription(scale.getDescription());
        dto.setQuestionCount(scale.getQuestionCount());
        dto.setEstimatedMinutes(scale.getEstimatedMinutes());
        return dto;
    }

    /**
     * 量表 Entity → 详情 DTO（含题目与选项，解析 scoring_config）
     */
    public AssessmentScaleDetailDTO toScaleDetailDTO(AssessmentScale scale) {
        if (scale == null) {
            return null;
        }
        AssessmentScaleDetailDTO dto = new AssessmentScaleDetailDTO();
        dto.setId(scale.getId());
        dto.setCode(scale.getCode());
        dto.setName(scale.getName());
        dto.setCategory(scale.getCategory());
        dto.setDescription(scale.getDescription());
        dto.setQuestionCount(scale.getQuestionCount());
        dto.setEstimatedMinutes(scale.getEstimatedMinutes());

        // 解析题目与选项
        AssessmentScoringService.ScoringConfig config = scoringService.parseConfig(scale.getScoringConfig());
        List<AssessmentScaleDetailDTO.QuestionOption> questions = new ArrayList<>();
        if (config.getQuestions() != null) {
            for (AssessmentScoringService.ScoringQuestion q : config.getQuestions()) {
                AssessmentScaleDetailDTO.QuestionOption qo = new AssessmentScaleDetailDTO.QuestionOption();
                qo.setIndex(q.getIndex());
                qo.setText(q.getText());
                List<AssessmentScaleDetailDTO.Option> options = new ArrayList<>();
                if (q.getOptions() != null) {
                    for (AssessmentScoringService.ScoringOption opt : q.getOptions()) {
                        AssessmentScaleDetailDTO.Option o = new AssessmentScaleDetailDTO.Option();
                        o.setLabel(opt.getLabel());
                        o.setScore(opt.getScore());
                        options.add(o);
                    }
                }
                qo.setOptions(options);
                questions.add(qo);
            }
        }
        dto.setQuestions(questions);
        return dto;
    }

    /**
     * 记录 Entity → 统一详情 DTO（GET /records/{id} 与提交后返回）
     * <p>IN_PROGRESS 时填充 status/currentIndex/answers；COMPLETED 时填充报告字段
     *
     * @param scale       关联量表
     * @param record      作答记录
     * @param riskMessage 风险提示文案（service 层根据风险题组装，可为空）
     */
    public AssessmentRecordDetailDTO toDetailDTO(AssessmentScale scale, AssessmentRecord record, String riskMessage) {
        if (record == null) {
            return null;
        }
        AssessmentRecordDetailDTO dto = new AssessmentRecordDetailDTO();
        dto.setRecordId(record.getId());
        dto.setScaleId(record.getScaleId());
        dto.setStatus(record.getStatus());
        dto.setCurrentIndex(record.getCurrentIndex());
        dto.setCompletedAt(record.getCompletedAt());

        // 量表信息
        if (scale != null) {
            dto.setScaleId(scale.getId());
            dto.setScaleCode(scale.getCode());
            dto.setScaleName(scale.getName());
            dto.setScaleCategory(scale.getCategory());
            dto.setScaleDescription(scale.getDescription());
        }

        // 作答进度（解析 answers JSON）
        dto.setAnswers(parseAnswers(record.getAnswers()));

        // 报告字段（COMPLETED 时填充）
        if ("COMPLETED".equals(record.getStatus())) {
            dto.setTotalScore(record.getTotalScore());
            dto.setLevel(record.getLevel());
            AssessmentLevel lv = AssessmentLevel.fromCode(record.getLevel());
            if (lv != null) {
                dto.setLevelLabel(lv.getLabel());
                dto.setLevelColor(lv.getColor());
            } else {
                // MBTI 等非标准等级（如 "INTJ"、"ENFP"），直接用 level 作为标签
                dto.setLevelLabel(record.getLevel());
                dto.setLevelColor("#9B7DB8"); // 紫色，人格测试中性色
            }
            dto.setDimensionScores(parseDimensionScores(record.getDimensionScores()));

            Map<String, Object> reportMap = parseReportMap(record.getReport());
            if (reportMap != null) {
                dto.setInterpretation((String) reportMap.get("interpretation"));
                Object suggestions = reportMap.get("suggestions");
                if (suggestions instanceof List<?> list) {
                    List<String> suggList = new ArrayList<>();
                    for (Object o : list) {
                        if (o != null) {
                            suggList.add(o.toString());
                        }
                    }
                    dto.setSuggestions(suggList);
                }
            }
            dto.setRiskFlag(record.getRiskFlag() == null ? 0 : record.getRiskFlag());
            if (riskMessage != null && !riskMessage.isBlank()) {
                dto.setRiskMessage(riskMessage);
            }
            dto.setDisclaimer(DISCLAIMER);
        }
        return dto;
    }

    /**
     * 答案 DTO 列表 → JSON 字符串
     */
    public String answersToJson(List<AssessmentAnswerItemDTO> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(toAnswersMapList(answers));
        } catch (Exception e) {
            log.error("答案序列化失败", e);
            throw new BusinessException("答案保存失败");
        }
    }

    /**
     * 答案 DTO 列表 → List<Map<questionIndex, score>>（Redis 缓存格式）
     */
    public List<Map<String, Object>> answersToMap(List<AssessmentAnswerItemDTO> answers) {
        return toAnswersMapList(answers);
    }

    /**
     * List<Map<questionIndex, score>> → JSON 字符串
     */
    public String answersToJsonFromList(List<Map<String, Object>> answersList) {
        if (answersList == null || answersList.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(answersList);
        } catch (Exception e) {
            log.error("答案序列化失败", e);
            throw new BusinessException("答案保存失败");
        }
    }

    private List<Map<String, Object>> toAnswersMapList(List<AssessmentAnswerItemDTO> answers) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (answers == null) {
            return list;
        }
        for (AssessmentAnswerItemDTO a : answers) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("questionIndex", a.getQuestionIndex());
            m.put("score", a.getScore());
            list.add(m);
        }
        return list;
    }

    /**
     * 维度得分 Map → JSON 字符串
     */
    public String dimensionScoresToJson(Map<String, BigDecimal> dimensionScores) {
        if (dimensionScores == null || dimensionScores.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(dimensionScores);
        } catch (Exception e) {
            log.error("维度得分序列化失败", e);
            throw new BusinessException("测评结果保存失败");
        }
    }

    /**
     * 报告内容 Map → JSON 字符串
     */
    public String reportToJson(Map<String, Object> reportMap) {
        if (reportMap == null || reportMap.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(reportMap);
        } catch (Exception e) {
            log.error("报告序列化失败", e);
            throw new BusinessException("报告保存失败");
        }
    }

    /**
     * 解析答案 JSON → List<Map>（questionIndex + score）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseAnswers(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.error("答案解析失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析维度得分 JSON → Map
     */
    public Map<String, BigDecimal> parseDimensionScores(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, BigDecimal>>() {
            });
        } catch (Exception e) {
            log.error("维度得分解析失败", e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 解析报告 JSON → Map
     */
    public Map<String, Object> parseReportMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("报告解析失败", e);
            return new LinkedHashMap<>();
        }
    }
}
