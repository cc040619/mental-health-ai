package com.cc.service;

import com.cc.common.RedisConstants;
import com.cc.dto.request.AiConfigSaveDTO;
import com.cc.entity.AiConfig;
import com.cc.exception.BusinessException;
import com.cc.mapper.AiConfigMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 配置业务层：模型参数 KV 读写（Redis 缓存 TTL 60s，写后删）+ per-request options 构造 +
 * 单价计算 + 实时测试限频与调用
 *
 * <p>模型参数通过 {@link OpenAiChatOptions} 每次调用覆盖（Spring AI 1.1 per-call options），
 * 管理端保存后下一条对话即生效，无需重启。读取全部走缓存，禁止 AI 调用路径每次查库；
 * 缓存/DB 故障 fail-open 返回空配置，走 application.yml 默认参数，不阻塞对话。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AiConfigService {

    public static final String KEY_MODEL = "MODEL";
    public static final String KEY_TEMPERATURE = "TEMPERATURE";
    public static final String KEY_MAX_TOKENS = "MAX_TOKENS";
    public static final String KEY_TOP_P = "TOP_P";
    public static final String KEY_INPUT_PRICE = "INPUT_PRICE";
    public static final String KEY_OUTPUT_PRICE = "OUTPUT_PRICE";
    public static final String KEY_FILTER_AI_REPLY = "FILTER_AI_REPLY";
    public static final String KEY_FILTER_ACTION = "FILTER_ACTION";

    /** 费用按百万 token 计价 */
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);

    @Resource
    private AiConfigMapper aiConfigMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;
    /**
     * @Lazy 打断循环依赖：aiCallLogService.stats() 反向依赖本类算费用，
     * 而本类仅实时测试时用它异步落库——延迟代理运行期解析，不影响正常启动
     */
    @Resource
    @Lazy
    private AiCallLogService aiCallLogService;
    @Resource
    @Qualifier("emotion-analysis")
    private ChatClient statelessChatClient;

    // ==================== 配置读取（带缓存） ====================

    /**
     * 读全部配置（整表缓存 JSON，TTL 60s）；缓存/DB 异常 fail-open 返回空 Map
     */
    public Map<String, String> getConfigMap() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.AI_CONFIG_CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<LinkedHashMap<String, String>>() {
                });
            }
        } catch (Exception e) {
            log.warn("AI 配置缓存读取异常，回源DB: {}", e.getMessage());
        }
        Map<String, String> configMap = new LinkedHashMap<>();
        try {
            List<AiConfig> rows = aiConfigMapper.selectList(null);
            for (AiConfig row : rows) {
                if (row.getConfigValue() != null) {
                    configMap.put(row.getConfigKey(), row.getConfigValue());
                }
            }
            stringRedisTemplate.opsForValue().set(RedisConstants.AI_CONFIG_CACHE_KEY,
                    objectMapper.writeValueAsString(configMap), Duration.ofSeconds(RedisConstants.AI_CACHE_TTL_SECONDS));
        } catch (Exception e) {
            log.warn("AI 配置读取失败（fail-open，走 yml 默认参数）: {}", e.getMessage());
        }
        return configMap;
    }

    /** 读单个配置值，缺省返回默认值 */
    public String getValue(String key, String defaultValue) {
        String value = getConfigMap().get(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    /**
     * 构造 per-request 模型参数覆盖项（AI 三处调用点共用）：
     * 有配置则覆盖，无对应配置走 ChatClient 默认（application.yml）
     */
    public OpenAiChatOptions buildChatOptions() {
        Map<String, String> configMap = getConfigMap();
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        String model = configMap.get(KEY_MODEL);
        if (StringUtils.hasText(model)) {
            builder.model(model.trim());
        }
        Double temperature = parseDouble(configMap.get(KEY_TEMPERATURE));
        if (temperature != null) {
            builder.temperature(temperature);
        }
        Integer maxTokens = parseInteger(configMap.get(KEY_MAX_TOKENS));
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
        Double topP = parseDouble(configMap.get(KEY_TOP_P));
        if (topP != null) {
            builder.topP(topP);
        }
        return builder.build();
    }

    // ==================== 管理端 ====================

    /**
     * 保存配置：逐项校验后 upsert，写后删缓存（下一条对话即生效）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigs(List<AiConfigSaveDTO.ConfigItem> items) {
        for (AiConfigSaveDTO.ConfigItem item : items) {
            String key = item.getConfigKey().trim();
            validateValue(key, item.getConfigValue());
            AiConfig existing = aiConfigMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConfig>()
                            .eq(AiConfig::getConfigKey, key));
            if (existing != null) {
                AiConfig update = AiConfig.builder()
                        .id(existing.getId())
                        .configValue(item.getConfigValue())
                        .build();
                aiConfigMapper.updateById(update);
            } else {
                aiConfigMapper.insert(AiConfig.builder()
                        .configKey(key)
                        .configValue(item.getConfigValue())
                        .build());
            }
        }
        evictConfigCache();
        log.info("保存AI配置: keys={}, operator=admin",
                items.stream().map(i -> i.getConfigKey()).toList());
    }

    /**
     * 实时测试（无 system，配置测试入口）
     */
    public Map<String, Object> test(String message) {
        return test(null, message);
    }

    /**
     * 实时测试/提示词试跑：当前配置 + 无记忆单轮调用；返回回复/耗时/模型/token；
     * usage 可提取时手动记日志（AI_TEST/PROMPT_TEST，带 token 费用可算）
     */
    public Map<String, Object> test(String systemContent, String message) {
        String msg = StringUtils.hasText(message) ? message : "你好，介绍一下你自己";
        String purpose = StringUtils.hasText(systemContent) ? "PROMPT_TEST" : "AI_TEST";
        String model = getValue(KEY_MODEL, "deepseek-v3");
        long start = System.currentTimeMillis();
        try {
            var spec = statelessChatClient.prompt()
                    .options(buildChatOptions());
            if (StringUtils.hasText(systemContent)) {
                spec = spec.system(systemContent);
            }
            ChatResponse response = spec
                    .user(msg)
                    .call()
                    .chatResponse();
            long durationMs = System.currentTimeMillis() - start;
            if (response == null || response.getResult() == null) {
                throw new BusinessException("AI 未返回内容，请检查模型配置");
            }
            String reply = response.getResult().getOutput().getText();
            // usage 尽力提取（有则费用可算；手动调用通常可取到）
            Integer inputTokens = null;
            Integer outputTokens = null;
            ChatResponseMetadata metadata = response.getMetadata();
            if (metadata != null && metadata.getUsage() != null) {
                Usage usage = metadata.getUsage();
                inputTokens = usage.getPromptTokens();
                outputTokens = usage.getCompletionTokens();
            }
            aiCallLogService.logAsync(purpose, model, inputTokens, outputTokens,
                    (int) durationMs, 1, null);
            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply != null ? reply : "");
            result.put("durationMs", durationMs);
            result.put("model", model);
            result.put("inputTokens", inputTokens);
            result.put("outputTokens", outputTokens);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - start;
            aiCallLogService.logAsync(purpose, model, null, null,
                    (int) durationMs, 0, truncate(e.getMessage(), 200));
            log.warn("AI 实时测试失败: {}", e.getMessage());
            throw new BusinessException("测试调用失败，请检查 API Key 与模型配置");
        }
    }

    // ==================== 限频与费用 ====================

    /**
     * 实时测试/试跑限频：同一管理员 10 秒窗口一次（真实消耗 token，防连点刷量）
     * ponytail: 单机 Redis setNX 限频，分布式场景够用；更精细限流需要时换 Bucket4j
     */
    public void checkTestLimit(Long adminId) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.AI_TEST_LIMIT_PREFIX + adminId, "1",
                Duration.ofSeconds(RedisConstants.AI_TEST_LIMIT_SECONDS));
        if (!Boolean.TRUE.equals(ok)) {
            throw new BusinessException("操作过于频繁，请 " + RedisConstants.AI_TEST_LIMIT_SECONDS + " 秒后再试");
        }
    }

    /**
     * 费用计算：input×输入单价 + output×输出单价（单价为元/百万 token），保留 4 位小数；
     * token 不可用（流式调用）时返回 null
     */
    public BigDecimal calcCost(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        Map<String, String> configMap = getConfigMap();
        BigDecimal inputPrice = parseDecimal(configMap.get(KEY_INPUT_PRICE), BigDecimal.ZERO);
        BigDecimal outputPrice = parseDecimal(configMap.get(KEY_OUTPUT_PRICE), BigDecimal.ZERO);
        BigDecimal in = BigDecimal.valueOf(inputTokens != null ? inputTokens : 0);
        BigDecimal out = BigDecimal.valueOf(outputTokens != null ? outputTokens : 0);
        return in.multiply(inputPrice).add(out.multiply(outputPrice))
                .divide(MILLION, 4, RoundingMode.HALF_UP);
    }

    // ==================== 私有工具 ====================

    /**
     * 配置值范围校验（后端硬校验，前端仅展示）：temperature 0-2 / max_tokens 1-8192 /
     * top_p 0-1 / 单价 ≥0 / MODEL 非空且格式合法 / 过滤开关与动作枚举
     */
    private void validateValue(String key, String value) {
        if (KEY_MODEL.equals(key)) {
            if (!StringUtils.hasText(value)) {
                throw new BusinessException("模型名不能为空");
            }
            // 白名单格式：字母数字与 -._，防任意注入
            if (!value.trim().matches("[A-Za-z0-9._-]{1,100}")) {
                throw new BusinessException("模型名格式非法");
            }
            return;
        }
        if (!StringUtils.hasText(value)) {
            return; // 其余键允许置空（走 yml 默认）
        }
        switch (key) {
            case KEY_TEMPERATURE -> checkRange(parseDouble(value), 0.0, 2.0, "温度需在 0-2 之间");
            case KEY_TOP_P -> checkRange(parseDouble(value), 0.0, 1.0, "top_p 需在 0-1 之间");
            case KEY_MAX_TOKENS -> {
                Integer maxTokens = parseInteger(value);
                if (maxTokens == null || maxTokens < 1 || maxTokens > 8192) {
                    throw new BusinessException("最大 token 需在 1-8192 之间");
                }
            }
            case KEY_INPUT_PRICE, KEY_OUTPUT_PRICE -> {
                BigDecimal price = parseDecimal(value, null);
                if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("单价不能为负数");
                }
            }
            case KEY_FILTER_AI_REPLY -> {
                if (!"ON".equals(value) && !"OFF".equals(value)) {
                    throw new BusinessException("过滤开关仅支持 ON/OFF");
                }
            }
            case KEY_FILTER_ACTION -> {
                if (!"BLOCK".equals(value) && !"REPLACE".equals(value) && !"LOG_ONLY".equals(value)) {
                    throw new BusinessException("过滤动作仅支持 BLOCK/REPLACE/LOG_ONLY");
                }
            }
            default -> {
                // 未知键不限制（保留扩展）
            }
        }
    }

    private void checkRange(Double value, double min, double max, String message) {
        if (value == null || value < min || value > max) {
            throw new BusinessException(message);
        }
    }

    private void evictConfigCache() {
        try {
            stringRedisTemplate.delete(RedisConstants.AI_CONFIG_CACHE_KEY);
        } catch (Exception e) {
            log.warn("删除AI配置缓存失败（fail-open，等 60s 过期）: {}", e.getMessage());
        }
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value, BigDecimal defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
