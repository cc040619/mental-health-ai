package com.cc.service;

import com.cc.entity.AiCallLog;
import com.cc.exception.BusinessException;
import com.cc.mapper.AiCallLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 调用日志业务层：AOP 埋点异步落库 + 统计聚合
 *
 * <p>口径：调用次数/耗时/成功率精确（AOP 记录方法调用耗时，流式方法为调用建立耗时、
 * 非流式总耗时）；token/费用尽力提取，无则 NULL（流式响应 usage 难取，毕设口径），
 * 升级路径=ChatModel 观测点或网关计费。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AiCallLogService {

    @Resource
    private AiCallLogMapper aiCallLogMapper;

    @Resource
    private AiConfigService aiConfigService;

    /**
     * 异步写调用日志（不阻塞对话流）；日志写失败仅告警，不影响 AI 主流程
     */
    @Async
    public void logAsync(String purpose, String model, Integer inputTokens, Integer outputTokens,
                         int durationMs, int success, String errorMsg) {
        try {
            aiCallLogMapper.insert(AiCallLog.builder()
                    .purpose(purpose)
                    .model(model)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .durationMs(durationMs)
                    .success(success)
                    .errorMsg(errorMsg)
                    .build());
        } catch (Exception e) {
            log.warn("AI调用日志写入失败: purpose={}, error={}", purpose, e.getMessage());
        }
    }

    /**
     * 统计聚合：调用次数/成功率/平均耗时/token/费用汇总 + 按日趋势 + 用途分布；
     * 直接查 ai_call_log 聚合（量小不缓存，量大时加日汇总表或缓存）
     *
     * @param range 7D / 30D
     */
    public Map<String, Object> stats(String range) {
        int days = "30D".equals(range) ? 30 : 7;
        String since = LocalDate.now().minusDays(days - 1L).atStartOfDay().toString();

        List<Map<String, Object>> dailyRows = aiCallLogMapper.selectDailyStats(since);
        List<Map<String, Object>> purposeRows = aiCallLogMapper.selectPurposeStats(since);

        // 汇总 + 每日费用（token 可用时按单价折算）
        long totalCount = 0;
        long successCount = 0;
        double durationSum = 0;
        long inputTokens = 0;
        long outputTokens = 0;
        boolean hasTokens = false;
        List<Map<String, Object>> daily = new ArrayList<>();
        for (Map<String, Object> row : dailyRows) {
            long count = toLong(row.get("count"));
            long ok = toLong(row.get("successCount"));
            Integer in = toInteger(row.get("inputTokens"));
            Integer out = toInteger(row.get("outputTokens"));
            BigDecimal cost = aiConfigService.calcCost(in, out);
            totalCount += count;
            successCount += ok;
            durationSum += toDouble(row.get("avgDurationMs")) * count;
            if (in != null) {
                inputTokens += in;
                hasTokens = true;
            }
            if (out != null) {
                outputTokens += out;
                hasTokens = true;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", String.valueOf(row.get("date")));
            item.put("count", count);
            item.put("successCount", ok);
            item.put("avgDurationMs", toDouble(row.get("avgDurationMs")));
            item.put("inputTokens", in);
            item.put("outputTokens", out);
            item.put("cost", cost);
            daily.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("successRate", totalCount > 0 ? Math.round(successCount * 1000.0 / totalCount) / 10.0 : 100.0);
        summary.put("avgDurationMs", totalCount > 0 ? Math.round(durationSum / totalCount) : 0);
        summary.put("inputTokens", hasTokens ? inputTokens : null);
        summary.put("outputTokens", hasTokens ? outputTokens : null);
        // 总费用：token 可用的调用按单价折算，token 全程不可用则为 null（口径见类注释）
        summary.put("totalCost", hasTokens ? aiConfigService.calcCost((int) inputTokens, (int) outputTokens) : null);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("daily", daily);
        result.put("purposes", purposeRows);
        return result;
    }

    /**
     * 校验统计范围参数
     */
    public static String normalizeRange(String range) {
        if (range != null && "30D".equals(range)) {
            return "30D";
        }
        if (range != null && !"7D".equals(range)) {
            throw new BusinessException("统计范围仅支持 7D/30D");
        }
        return "7D";
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0d;
    }
}
