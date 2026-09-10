package com.cc.AIservice;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.dto.response.ConsultationMessageResponseDTO;
import com.cc.dto.response.SessionSummaryResponseDTO;
import com.cc.entity.ConsultationSession;
import com.cc.mapper.ConsultationSessionMapper;
import com.cc.service.ConsultationMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话总结服务：会话结束后异步调用 AI 生成摘要/核心问题/建议清单，结果落库到
 * consultation_session.summary。
 *
 * <p>全部失败路径静默降级（仅记日志，不写 summary），不阻塞任何主流程。
 * 复用无记忆、无默认系统提示词的 emotion-analysis ChatClient 做一次性结构化调用。</p>
 *
 * @author cc
 * @date 2026-09-09
 */
@Slf4j
@Service
public class ConsultationSummaryService {

    /** 送给 AI 的对话上下文最大字符数，避免超出上下文窗口 */
    private static final int MAX_CONTEXT_TEXT_LENGTH = 6000;

    /** 生成总结所需的最少消息数（1条用户消息 + 1条AI回复） */
    private static final int MIN_MESSAGE_COUNT = 2;

    @Resource
    @Qualifier("emotion-analysis")
    private ChatClient statelessChatClient;

    @Resource
    private ConsultationMessageService consultationMessageService;

    @Resource
    private ConsultationSessionMapper consultationSessionMapper;

    /**
     * 异步生成会话总结并落库
     *
     * @param sessionId 数据库会话ID
     */
    @Async
    public void generateSummaryAsync(Long sessionId) {
        try {
            ConsultationSession session = consultationSessionMapper.selectById(sessionId);
            if (session == null) {
                log.warn("会话总结跳过：会话不存在, sessionId={}", sessionId);
                return;
            }
            // 幂等：已有总结不再重复生成
            if (StringUtils.hasText(session.getSummary())) {
                log.info("会话总结已存在，跳过生成, sessionId={}", sessionId);
                return;
            }

            // 拼接对话上下文（按时间升序）
            List<ConsultationMessageResponseDTO> messages = consultationMessageService.listBySessionId(sessionId);
            if (messages.size() < MIN_MESSAGE_COUNT) {
                log.warn("会话总结跳过：对话内容不足, sessionId={}", sessionId);
                return;
            }
            StringBuilder transcript = new StringBuilder();
            for (ConsultationMessageResponseDTO message : messages) {
                String speaker = message.getSenderType() != null && message.getSenderType() == 2 ? "AI助手" : "用户";
                transcript.append(speaker).append(": ").append(message.getContent()).append("\n");
            }

            SessionSummaryResponseDTO summary = requestSummary(transcript.toString());
            if (summary == null) {
                // AI 返回无法解析时静默降级：summary 留空，前端可再次触发
                log.warn("会话总结生成失败：AI返回内容无法解析, sessionId={}", sessionId);
                return;
            }

            // 落库（status 为推导字段，不入库）
            summary.setStatus(null);
            ConsultationSession update = ConsultationSession.builder()
                    .id(sessionId)
                    .summary(JSONUtil.toJsonStr(summary))
                    .build();
            consultationSessionMapper.updateById(update);
            log.info("会话总结生成完成, sessionId={}", sessionId);
        } catch (Exception e) {
            // 异步任务失败仅记录日志，不影响主流程
            log.error("会话总结生成异常, sessionId={}", sessionId, e);
        }
    }

    /**
     * 调用 AI 生成总结，返回结构化 DTO；入参为空或解析失败返回 null
     */
    private SessionSummaryResponseDTO requestSummary(String transcript) {
        if (!StringUtils.hasText(transcript)) {
            return null;
        }
        String context = transcript.length() > MAX_CONTEXT_TEXT_LENGTH
                ? transcript.substring(transcript.length() - MAX_CONTEXT_TEXT_LENGTH)
                : transcript;

        String aiResponse = statelessChatClient.prompt()
                .system(PromptManage.SESSION_SUMMARY_SYSTEM_PROMPT)
                .user("对话记录如下：\n" + context)
                .call()
                .content();

        return parseSummary(aiResponse);
    }

    /**
     * 解析 AI 返回的 JSON 文本为总结 DTO，截取首个 { 到最后一个 } 之间的内容
     */
    private SessionSummaryResponseDTO parseSummary(String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            return null;
        }
        try {
            String json = aiResponse.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            JSONObject jsonObject = JSONUtil.parseObj(json.substring(start, end + 1));

            String summary = jsonObject.getStr("summary");
            if (!StringUtils.hasText(summary)) {
                return null;
            }
            List<String> coreIssues = toStringList(jsonObject.getJSONArray("coreIssues"));
            List<String> suggestions = toStringList(jsonObject.getJSONArray("suggestions"));

            return SessionSummaryResponseDTO.builder()
                    .summary(summary.trim())
                    .coreIssues(coreIssues)
                    .suggestions(suggestions)
                    .generatedAt(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.warn("会话总结 JSON 解析失败");
            return null;
        }
    }

    /**
     * JSON 数组转字符串列表，空值/异常返回空列表
     */
    private List<String> toStringList(JSONArray array) {
        List<String> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (Object item : array) {
            if (item != null) {
                String text = item.toString().trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }
        return result;
    }
}
