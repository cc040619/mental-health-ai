package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AiBlockRecordResponseDTO;
import com.cc.entity.AiBlockRecord;
import com.cc.mapper.AiBlockRecordMapper;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 回复敏感词过滤：开关（FILTER_AI_REPLY）+ 动作（FILTER_ACTION）从 ai_config 读（缓存 60s）
 *
 * <p>复用 {@link SensitiveWordFilter#findSensitiveWord}（4.7 表驱动词库，DFA 只探测不抛异常）。
 * 危机词不在此过滤范围——crisis-words.txt 独立词树不受管理端开关影响，
 * 危机干预走 3.9 既有流程（安全底线）。</p>
 *
 * <p>动作口径（作用于入库内容，前端流式已看到的原文不受影响）：
 * BLOCK=整条替换为审核提示语；REPLACE=命中词打码；LOG_ONLY=仅记录不改动。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AiReplyFilterService {

    /** BLOCK 动作的替换文案 */
    private static final String BLOCK_TEXT = "抱歉，这条回复内容需要审核后展示";

    /** 拦截记录摘要最大长度（不存全文——心理健康对话属敏感数据） */
    private static final int SUMMARY_MAX = 200;

    @Resource
    private AiConfigService aiConfigService;
    @Resource
    private SensitiveWordFilter sensitiveWordFilter;
    @Resource
    private AiBlockRecordMapper aiBlockRecordMapper;

    /**
     * 过滤 AI 回复：开关 OFF 原样返回；命中按 FILTER_ACTION 处理并写拦截记录（只存摘要）
     *
     * @param sessionId 会话ID（可空）
     * @param content   AI 回复全文
     * @return 处理后的内容
     */
    public String filter(Long sessionId, String content) {
        if (!StringUtils.hasText(content) || !"ON".equals(aiConfigService.getValue(AiConfigService.KEY_FILTER_AI_REPLY, "OFF"))) {
            return content;
        }
        String action = aiConfigService.getValue(AiConfigService.KEY_FILTER_ACTION, "LOG_ONLY");
        String result = content;
        String firstHit = null;
        // 循环探测替换直到无命中（REPLACE 逐词打码；BLOCK/LOG_ONLY 首次命中即终止）
        String hit;
        while ((hit = sensitiveWordFilter.findSensitiveWord(result)) != null) {
            if (firstHit == null) {
                firstHit = hit;
            }
            if ("BLOCK".equals(action)) {
                result = BLOCK_TEXT;
                break;
            }
            if ("REPLACE".equals(action)) {
                result = result.replace(hit, "*".repeat(hit.length()));
            } else {
                break; // LOG_ONLY 不处理
            }
        }
        if (firstHit != null) {
            aiBlockRecordMapper.insert(AiBlockRecord.builder()
                    .sessionId(sessionId)
                    .triggerWord(firstHit)
                    .contentSummary(result.length() <= SUMMARY_MAX ? result : result.substring(0, SUMMARY_MAX))
                    .action(action)
                    .build());
            // 日志不打印对话内容，只记会话与动作
            log.info("AI 回复命中敏感词: sessionId={}, action={}", sessionId, action);
        }
        return result;
    }

    /**
     * 拦截记录分页（时间倒序，size≤20）
     */
    public Map<String, Object> pageBlocks(Long page, Long size) {
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 20L) : 10L;
        Page<AiBlockRecord> result = aiBlockRecordMapper.selectPage(new Page<>(cur, sz),
                new LambdaQueryWrapper<AiBlockRecord>().orderByDesc(AiBlockRecord::getCreatedAt));
        List<AiBlockRecordResponseDTO> records = result.getRecords().stream().map(r -> {
            AiBlockRecordResponseDTO dto = new AiBlockRecordResponseDTO();
            dto.setId(r.getId());
            dto.setSessionId(r.getSessionId());
            dto.setTriggerWord(r.getTriggerWord());
            dto.setContentSummary(r.getContentSummary());
            dto.setAction(r.getAction());
            dto.setCreatedAt(r.getCreatedAt());
            return dto;
        }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return data;
    }
}
