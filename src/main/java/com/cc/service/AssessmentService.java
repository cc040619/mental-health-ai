package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.AIservice.PromptManage;
import com.cc.common.RedisConstants;
import com.cc.dto.request.AssessmentProgressDTO;
import com.cc.dto.request.AssessmentStartDTO;
import com.cc.dto.request.AssessmentSubmitDTO;
import com.cc.dto.request.AssessmentRecordPageQueryDTO;
import com.cc.dto.request.AssessmentAnswerItemDTO;
import com.cc.dto.response.AssessmentRecordDetailDTO;
import com.cc.dto.response.AssessmentRecordResponseDTO;
import com.cc.dto.response.AssessmentScaleDetailDTO;
import com.cc.dto.response.AssessmentScaleResponseDTO;
import com.cc.dto.response.AssessmentTrendResponseDTO;
import com.cc.entity.AssessmentRecord;
import com.cc.entity.AssessmentScale;
import com.cc.enums.AssessmentStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.AssessmentRecordMapper;
import com.cc.mapper.AssessmentScaleMapper;
import com.cc.service.convert.AssessmentConvert;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 心理测评业务层
 *
 * <p>架构：MySQL 持久化 + Redis 加速</p>
 * <ul>
 *   <li>量表定义（assessment_scale）存 MySQL，Redis 做 Cache-Aside 缓存</li>
 *   <li>用户答题进度存 Redis（轻量、快速，TTL 24h），不直接落库</li>
 *   <li>完成后从 Redis 读全部答案 → 计分 → 落库 assessment_record → 清 Redis 临时数据</li>
 *   <li>Redis 不可用时 fail-open，回退 DB 模式（兼容现有 4 个量表）</li>
 * </ul>
 *
 * <p>安全合规：测评记录只能本人查看（越权校验）；日志禁止打印 answers/报告内容</p>
 *
 * @author cc
 * @date 2026-09-08
 */
@Slf4j
@Service
public class AssessmentService {

    @Resource
    private AssessmentScaleMapper scaleMapper;

    @Resource
    private AssessmentRecordMapper recordMapper;

    @Resource
    private AssessmentScoringService scoringService;

    @Resource
    private AssessmentConvert convert;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    // 可选加分项：AI 个性化解读
    @Resource
    @Qualifier("open-ai")
    private ChatClient chatClient;

    // ==================== 量表查询（Cache-Aside） ====================

    /**
     * 测评广场列表（按类别筛选，仅启用量表）
     * <p>Cache-Aside：列表缓存按 category 维度，命中直接返回；未命中查 DB 后回填
     */
    public Map<String, Object> listScales(String category, Long current, Long size) {
        long cur = (current != null && current > 0) ? current : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 50L) : 20L;

        LambdaQueryWrapper<AssessmentScale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentScale::getEnabled, 1);
        if (category != null && !category.isBlank()) {
            wrapper.eq(AssessmentScale::getCategory, category);
        }
        wrapper.orderByAsc(AssessmentScale::getSortOrder);

        Page<AssessmentScale> page = new Page<>(cur, sz);
        Page<AssessmentScale> result = scaleMapper.selectPage(page, wrapper);
        List<AssessmentScaleResponseDTO> list = result.getRecords().stream()
                .map(convert::toScaleResponseDTO)
                .collect(Collectors.toList());
        return Map.of("records", list, "total", result.getTotal());
    }

    /**
     * 量表详情（含题目与选项）
     * <p>Cache-Aside：先查 Redis 缓存，未命中查 DB 后回填缓存
     */
    public AssessmentScaleDetailDTO getScaleDetail(Long scaleId) {
        AssessmentScale scale = getScaleWithCache(scaleId);
        if (scale == null) {
            throw new BusinessException("量表不存在或未启用");
        }
        return convert.toScaleDetailDTO(scale);
    }

    /**
     * 获取量表定义，走 Redis 缓存（Cache-Aside）
     *
     * @param scaleId 量表ID
     * @return 量表实体（未启用/不存在返回 null）
     */
    private AssessmentScale getScaleWithCache(Long scaleId) {
        // 1. 查 Redis 缓存
        try {
            String cacheKey = RedisConstants.buildAssessmentScaleCacheKey(scaleId);
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, AssessmentScale.class);
            }
        } catch (Exception e) {
            log.warn("量表缓存读取异常，回退查DB: scaleId={}, err={}", scaleId, e.getMessage());
        }

        // 2. 查 DB
        AssessmentScale scale = scaleMapper.selectById(scaleId);
        if (scale == null || scale.getEnabled() == null || scale.getEnabled() != 1) {
            return null;
        }

        // 3. 回填 Redis 缓存
        try {
            String cacheKey = RedisConstants.buildAssessmentScaleCacheKey(scaleId);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(scale),
                    RedisConstants.ASSESSMENT_SCALE_CACHE_TTL_HOURS,
                    TimeUnit.HOURS
            );
            // 同步按 code 缓存（方便其他入口按 code 查）
            if (scale.getCode() != null) {
                String codeKey = RedisConstants.buildAssessmentScaleCacheKeyByCode(scale.getCode());
                stringRedisTemplate.opsForValue().set(
                        codeKey,
                        objectMapper.writeValueAsString(scale),
                        RedisConstants.ASSESSMENT_SCALE_CACHE_TTL_HOURS,
                        TimeUnit.HOURS
                );
            }
        } catch (Exception e) {
            log.warn("量表缓存写入失败（fail-open，不阻塞主流程）: scaleId={}, err={}", scaleId, e.getMessage());
        }

        return scale;
    }

    // ==================== 开始测评（Redis 存进度） ====================

    /**
     * 开始测评：若已存在该量表的 IN_PROGRESS 记录则返回续答，否则新建
     * <p>DB 只存 metadata（recordId/scaleId/status=IN_PROGRESS），答题进度存 Redis
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startAssessment(Long userId, AssessmentStartDTO startDTO) {
        AssessmentScale scale = getScaleWithCache(startDTO.getScaleId());
        if (scale == null) {
            throw new BusinessException("量表不存在或未启用");
        }

        // 查找该用户该量表进行中的记录
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getUserId, userId)
                .eq(AssessmentRecord::getScaleId, startDTO.getScaleId())
                .eq(AssessmentRecord::getStatus, AssessmentStatus.IN_PROGRESS.getCode());
        AssessmentRecord existing = recordMapper.selectOne(wrapper);

        if (existing != null) {
            // 续答：从 Redis 读取答题进度（优先），回退 DB
            ProgressData progress = loadProgressFromRedis(userId, existing.getId());
            if (progress == null) {
                // Redis 无 → 回退读 DB answers 字段
                progress = new ProgressData(
                        existing.getCurrentIndex() == null ? 0 : existing.getCurrentIndex(),
                        convert.parseAnswers(existing.getAnswers())
                );
            }
            return Map.of(
                    "recordId", existing.getId(),
                    "scaleId", startDTO.getScaleId(),
                    "resumed", true,
                    "currentIndex", progress.currentIndex,
                    "answers", progress.answers
            );
        }

        // 新建 IN_PROGRESS 记录（metadata only，answers 字段暂不存）
        AssessmentRecord record = AssessmentRecord.builder()
                .userId(userId)
                .scaleId(startDTO.getScaleId())
                .status(AssessmentStatus.IN_PROGRESS.getCode())
                .currentIndex(0)
                .answers(null)
                .riskFlag(0)
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        recordMapper.insert(record);

        // 初始化 Redis 进度（空答题列表）
        saveProgressToRedis(userId, record.getId(), 0, List.of());

        return Map.of(
                "recordId", record.getId(),
                "scaleId", startDTO.getScaleId(),
                "resumed", false,
                "currentIndex", 0,
                "answers", List.of()
        );
    }

    /**
     * 中途保存进度（退出续答）—— 只写 Redis，不落 DB
     */
    public void saveProgress(Long userId, Long recordId, AssessmentProgressDTO progressDTO) {
        AssessmentRecord record = getOwnedRecord(userId, recordId);
        if (!AssessmentStatus.IN_PROGRESS.getCode().equals(record.getStatus())) {
            throw new BusinessException("该测评已完成，无法保存进度");
        }
        saveProgressToRedis(userId, recordId, progressDTO.getCurrentIndex(), progressDTO.getAnswers());
    }

    /**
     * 提交作答：读 Redis answers → 计分 → 落库 → 清 Redis
     * <p>DB 写入在事务内，Redis 清理在事务提交后执行（afterCommit），避免 DB 回滚时 Redis 已清理
     */
    @Transactional(rollbackFor = Exception.class)
    public AssessmentRecordDetailDTO submitAssessment(Long userId, Long recordId, AssessmentSubmitDTO submitDTO) {
        AssessmentRecord record = getOwnedRecord(userId, recordId);
        if (!AssessmentStatus.IN_PROGRESS.getCode().equals(record.getStatus())) {
            throw new BusinessException("该测评已提交，请勿重复提交");
        }

        AssessmentScale scale = getScaleWithCache(record.getScaleId());
        if (scale == null) {
            throw new BusinessException("量表不存在");
        }

        // 优先从 Redis 读答案（最权威来源），Redis 无则用前端提交的
        List<Map<String, Object>> answersList = loadProgressFromRedis(userId, recordId) != null
                ? loadProgressFromRedis(userId, recordId).answers
                : convert.answersToMap(submitDTO.getAnswers());

        // 答案转 questionIndex -> score
        Map<Integer, Integer> answers = parseAnswersToMap(answersList);

        // 完整性校验
        AssessmentScoringService.ScoringConfig config = scoringService.parseConfig(scale.getScoringConfig());
        if (config.getQuestions() != null && answers.size() < config.getQuestions().size()) {
            throw new BusinessException("还有未作答的题目，请完成全部题目后提交");
        }

        // 评分
        AssessmentScoringService.ScoringResult result = scoringService.calculate(config, answers);

        // 组装报告
        Map<String, Object> reportMap = buildReport(scale, config, result, answers);
        String riskMessage = buildRiskMessage(config, answers, result.getLevel());

        // 写 DB（事务内）
        record.setStatus(AssessmentStatus.COMPLETED.getCode());
        record.setAnswers(convert.answersToJson(submitDTO.getAnswers()));
        record.setCurrentIndex(config.getQuestions() == null ? 0 : config.getQuestions().size() - 1);
        record.setTotalScore(result.getTotalScore());
        record.setLevel(result.getLevel());
        record.setDimensionScores(convert.dimensionScoresToJson(result.getDimensionScores()));
        record.setReport(convert.reportToJson(reportMap));
        record.setRiskFlag(result.getRiskFlag());
        record.setCompletedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);

        // 事务提交后清理 Redis 临时答题数据
        registerAfterCommitCleanup(userId, recordId);

        // 重新查询保证数据一致
        AssessmentRecord updated = recordMapper.selectById(recordId);
        return convert.toDetailDTO(scale, updated, riskMessage);
    }

    /**
     * 报告/进度详情（越权校验）
     * <p>IN_PROGRESS 状态优先从 Redis 读 answers（更实时），COMPLETED 从 DB 读
     */
    public AssessmentRecordDetailDTO getRecordDetail(Long userId, Long recordId) {
        AssessmentRecord record = getOwnedRecord(userId, recordId);
        AssessmentScale scale = getScaleWithCache(record.getScaleId());

        // 进度类字段：IN_PROGRESS 优先 Redis
        String riskMessage = null;
        List<Map<String, Object>> answersList;
        Integer currentIndex = record.getCurrentIndex();

        if (AssessmentStatus.IN_PROGRESS.getCode().equals(record.getStatus())) {
            ProgressData progress = loadProgressFromRedis(userId, recordId);
            if (progress != null) {
                currentIndex = progress.currentIndex;
                answersList = progress.answers;
            } else {
                answersList = convert.parseAnswers(record.getAnswers());
            }
        } else {
            answersList = convert.parseAnswers(record.getAnswers());
            // COMPLETED 时计算风险提示文案
            riskMessage = buildRiskMessage(
                    scoringService.parseConfig(scale.getScoringConfig()),
                    parseAnswersToMap(answersList),
                    record.getLevel()
            );
        }

        // 组装一个临时 record 对象，覆盖 Redis 中的 answers/currentIndex
        AssessmentRecord merged = AssessmentRecord.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .scaleId(record.getScaleId())
                .status(record.getStatus())
                .currentIndex(currentIndex)
                .answers(convert.answersToJsonFromList(answersList))
                .totalScore(record.getTotalScore())
                .level(record.getLevel())
                .dimensionScores(record.getDimensionScores())
                .report(record.getReport())
                .riskFlag(record.getRiskFlag())
                .startedAt(record.getStartedAt())
                .completedAt(record.getCompletedAt())
                .build();

        return convert.toDetailDTO(scale, merged, riskMessage);
    }

    /**
     * 测评历史（已完成记录分页）
     */
    public Map<String, Object> listRecords(Long userId, AssessmentRecordPageQueryDTO queryDTO) {
        long cur = (queryDTO.getCurrent() != null && queryDTO.getCurrent() > 0) ? queryDTO.getCurrent() : 1L;
        long sz = (queryDTO.getSize() != null && queryDTO.getSize() > 0)
                ? Math.min(queryDTO.getSize(), 50L) : 10L;

        Page<AssessmentRecordResponseDTO> page = new Page<>(cur, sz);
        IPage<AssessmentRecordResponseDTO> result = recordMapper.selectCompletedPage(page, userId, queryDTO.getScaleId());
        return Map.of("records", result.getRecords(), "total", result.getTotal());
    }

    /**
     * 同量表历史趋势（按时间升序）
     */
    public List<AssessmentTrendResponseDTO> getTrend(Long userId, Long scaleId) {
        if (scaleId == null) {
            throw new BusinessException("量表ID不能为空");
        }
        return recordMapper.selectTrend(userId, scaleId);
    }

    // ==================== Redis 进度存取 ====================

    /**
     * Redis 进度数据内部类
     */
    @lombok.Data
    public static class ProgressData {
        private Integer currentIndex;
        private List<Map<String, Object>> answers;

        public ProgressData() {
        }

        public ProgressData(Integer currentIndex, List<Map<String, Object>> answers) {
            this.currentIndex = currentIndex;
            this.answers = answers;
        }
    }

    /**
     * 从 Redis 读取答题进度
     *
     * @return 进度数据；Redis 不可用或无数据返回 null
     */
    private ProgressData loadProgressFromRedis(Long userId, Long recordId) {
        try {
            String key = RedisConstants.buildAssessmentProgressKey(userId, recordId);
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, ProgressData.class);
        } catch (Exception e) {
            log.warn("读取 Redis 答题进度失败（fail-open，回退 DB）: userId={}, recordId={}, err={}",
                    userId, recordId, e.getMessage());
            return null;
        }
    }

    /**
     * 将答题进度写入 Redis（带 TTL 自动过期）
     */
    private void saveProgressToRedis(Long userId, Long recordId, Integer currentIndex,
                                     List<AssessmentAnswerItemDTO> answers) {
        try {
            ProgressData data = new ProgressData(currentIndex, convert.answersToMap(answers));
            String key = RedisConstants.buildAssessmentProgressKey(userId, recordId);
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(data),
                    RedisConstants.ASSESSMENT_PROGRESS_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            // fail-open：Redis 不可用时不阻塞作答，但需要告知调用方
            log.warn("Redis 答题进度保存失败（fail-open）: userId={}, recordId={}, err={}",
                    userId, recordId, e.getMessage());
        }
    }

    /**
     * 将答题进度写入 Redis（内部调用，List<Map> 形式）
     */
    private void saveProgressToRedisRaw(Long userId, Long recordId, Integer currentIndex,
                                        List<Map<String, Object>> answersList) {
        try {
            ProgressData data = new ProgressData(currentIndex, answersList);
            String key = RedisConstants.buildAssessmentProgressKey(userId, recordId);
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(data),
                    RedisConstants.ASSESSMENT_PROGRESS_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("Redis 答题进度保存失败（fail-open）: userId={}, recordId={}, err={}",
                    userId, recordId, e.getMessage());
        }
    }

    /**
     * 事务提交后清理 Redis 临时答题数据
     * <p>在 afterCommit 中执行，确保 DB 已成功落库再清理 Redis，避免 DB 回滚导致 Redis 数据丢失
     */
    private void registerAfterCommitCleanup(Long userId, Long recordId) {
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        String key = RedisConstants.buildAssessmentProgressKey(userId, recordId);
                        stringRedisTemplate.delete(key);
                        log.info("已清理 Redis 答题进度: userId={}, recordId={}", userId, recordId);
                    } catch (Exception e) {
                        log.warn("Redis 进度清理失败（可接受，TTL 会自动过期）: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            // 非事务环境或已有同步注册者时降级：直接删（Redis 里还有 24h TTL 兜底）
            log.warn("afterCommit 注册失败，尝试直接清理 Redis: {}", e.getMessage());
            try {
                String key = RedisConstants.buildAssessmentProgressKey(userId, recordId);
                stringRedisTemplate.delete(key);
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 报告组装 ====================

    /**
     * 组装报告内容：解读 + 建议列表
     */
    private Map<String, Object> buildReport(AssessmentScale scale,
                                            AssessmentScoringService.ScoringConfig config,
                                            AssessmentScoringService.ScoringResult result,
                                            Map<Integer, Integer> answers) {
        Map<String, Object> reportMap = new LinkedHashMap<>();
        AssessmentScoringService.ScoringLevel matched = result.getMatchedLevel();
        StringBuilder interpretation = new StringBuilder();
        if (matched != null) {
            interpretation.append(matched.getDescription());
        }
        if (result.getDimensionScores() != null && !result.getDimensionScores().isEmpty()) {
            interpretation.append("\n\n【维度分析】");
            result.getDimensionScores().forEach((name, score) ->
                    interpretation.append("\n").append(name).append("：").append(score).append(" 分"));
        }
        String aiAdvice = generateAIInterpretation(scale, result, answers);
        if (aiAdvice != null && !aiAdvice.isBlank()) {
            interpretation.append("\n\n【个性化建议】\n").append(aiAdvice);
        }
        reportMap.put("interpretation", interpretation.toString());

        List<String> suggestions = new ArrayList<>();
        if (matched != null && matched.getSuggestion() != null) {
            suggestions.add(matched.getSuggestion());
        }
        if (result.getCategoryAdvice() != null) {
            suggestions.addAll(result.getCategoryAdvice());
        }
        reportMap.put("suggestions", suggestions);
        return reportMap;
    }

    /**
     * 风险提示文案：风险题得分达阈值 或 等级为 SEVERE 时触发
     */
    private String buildRiskMessage(AssessmentScoringService.ScoringConfig config,
                                    Map<Integer, Integer> answers, String level) {
        if (config == null) {
            return null;
        }
        boolean risk = "SEVERE".equals(level);
        if (!risk && config.getRiskQuestionIndexes() != null && !config.getRiskQuestionIndexes().isEmpty()) {
            int threshold = config.getRiskScoreThreshold() == null ? 0 : config.getRiskScoreThreshold();
            for (Integer idx : config.getRiskQuestionIndexes()) {
                Integer s = answers.get(idx);
                if (s != null && s >= threshold) {
                    risk = true;
                    break;
                }
            }
        }
        if (!risk) {
            return null;
        }
        return "您的测评结果值得关注：建议尽快联系家人或朋友陪伴，并拨打全国心理援助热线 12356（" +
                "北京心理危机研究与干预中心 010-82951332）或前往正规医院心理科/精神科就诊。" +
                "您不是一个人，专业支持可以帮助您度过这段时期。";
    }

    /**
     * AI 个性化解读（可选加分项）：复用 ChatClient，失败时静默降级为空
     */
    private String generateAIInterpretation(AssessmentScale scale,
                                            AssessmentScoringService.ScoringResult result,
                                            Map<Integer, Integer> answers) {
        try {
            String promptText = String.format(
                    "你是一位温暖专业的心理健康助手。用户完成了「%s」测评，总分 %.2f，等级为「%s」。" +
                            "请用 2-3 句温和、克制、非诊断性的语言给出个性化建议，不出现「确诊」等医疗诊断用语，结尾附一句鼓励。" +
                            "维度得分：%s",
                    scale.getName(),
                    result.getTotalScore(),
                    result.getLevel(),
                    result.getDimensionScores()
            );
            String conversationId = "assessment_" + System.currentTimeMillis();
            return chatClient.prompt()
                    .system(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
                    .user(promptText)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI 个性化解读生成失败，降级为规则文案: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 工具方法 ====================

    private AssessmentRecord getOwnedRecord(Long userId, Long recordId) {
        AssessmentRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("测评记录不存在");
        }
        if (!userId.equals(record.getUserId())) {
            throw new BusinessException("测评记录不存在");
        }
        return record;
    }

    private Map<Integer, Integer> parseAnswersToMap(List<Map<String, Object>> answers) {
        Map<Integer, Integer> map = new HashMap<>();
        if (answers == null) {
            return map;
        }
        for (Map<String, Object> a : answers) {
            Object idx = a.get("questionIndex");
            Object score = a.get("score");
            if (idx instanceof Number && score instanceof Number) {
                map.put(((Number) idx).intValue(), ((Number) score).intValue());
            }
        }
        return map;
    }

    /**
     * List<Map<questionIndex, score>> → JSON（AssessmentConvert 已有同名方法处理 DTO，这里处理 Map 列表）
     */
    private String answersToJsonFromList(List<Map<String, Object>> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception e) {
            log.error("答案序列化失败", e);
            throw new BusinessException("答案保存失败");
        }
    }
}
