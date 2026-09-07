package com.cc.AIservice;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.dto.response.ConsultationMessageResponseDTO;
import com.cc.dto.response.EmotionAnalysisDTO;
import com.cc.entity.ConsultationSession;
import com.cc.entity.EmotionDiary;
import com.cc.mapper.ConsultationSessionMapper;
import com.cc.mapper.EmotionDiaryMapper;
import com.cc.service.ConsultationMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 情绪分析服务，AI 异步分析会话对话与情绪日记，并将 JSON 结果落库
 *
 * <p>会话分析结果写入 consultation_session.last_emotion_analysis，
 * 日记分析结果写入 emotion_diary.ai_emotion_analysis，前端查询时直接读取存储结果。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@Slf4j
@Service
public class EmotionAnalysisService {

    /** 单次分析送给 AI 的最大文本长度，避免超出上下文窗口 */
    private static final int MAX_ANALYSIS_TEXT_LENGTH = 6000;

    @Resource
    @Qualifier("emotion-analysis")
    private ChatClient analysisChatClient;

    @Resource
    private ConsultationMessageService consultationMessageService;

    @Resource
    private ConsultationSessionMapper consultationSessionMapper;

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    /**
     * 异步分析咨询会话，结果更新到会话的 last_emotion_analysis
     *
     * @param sessionId 数据库会话ID
     */
    @Async
    public void analyzeSessionAsync(Long sessionId) {
        try {
            ConsultationSession session = consultationSessionMapper.selectById(sessionId);
            if (session == null) {
                log.warn("情绪分析跳过：会话不存在, sessionId={}", sessionId);
                return;
            }

            // 拼接对话内容（按时间升序）
            List<ConsultationMessageResponseDTO> messages = consultationMessageService.listBySessionId(sessionId);
            StringBuilder transcript = new StringBuilder();
            for (ConsultationMessageResponseDTO message : messages) {
                String speaker = message.getSenderType() != null && message.getSenderType() == 2 ? "AI助手" : "用户";
                transcript.append(speaker).append(": ").append(message.getContent()).append("\n");
            }

            EmotionAnalysisDTO analysis = analyzeText(transcript.toString());
            if (analysis == null) {
                log.warn("情绪分析失败：AI返回内容无法解析, sessionId={}", sessionId);
                return;
            }

            ConsultationSession update = ConsultationSession.builder()
                    .id(sessionId)
                    .lastEmotionAnalysis(JSONUtil.toJsonStr(analysis))
                    .lastEmotionUpdatedAt(LocalDateTime.now())
                    .build();
            consultationSessionMapper.updateById(update);
            log.info("会话情绪分析完成, sessionId={}", sessionId);
        } catch (Exception e) {
            // 异步任务失败仅记录日志，不影响主流程
            log.error("会话情绪分析异常, sessionId={}", sessionId, e);
        }
    }

    /**
     * 异步分析情绪日记，结果更新到日记的 ai_emotion_analysis
     *
     * @param diaryId 日记ID
     */
    @Async
    public void analyzeDiaryAsync(Long diaryId) {
        try {
            EmotionDiary diary = emotionDiaryMapper.selectById(diaryId);
            if (diary == null) {
                log.warn("情绪分析跳过：日记不存在, diaryId={}", diaryId);
                return;
            }

            // 拼接日记上下文
            StringBuilder content = new StringBuilder();
            content.append("日记日期: ").append(diary.getDiaryDate()).append("\n");
            content.append("情绪评分(1-10): ").append(diary.getMoodScore()).append("\n");
            content.append("主要情绪: ").append(diary.getDominantEmotion()).append("\n");
            if (StringUtils.hasText(diary.getEmotionTriggers())) {
                content.append("情绪触发因素: ").append(diary.getEmotionTriggers()).append("\n");
            }
            if (diary.getSleepQuality() != null) {
                content.append("睡眠质量(1-5): ").append(diary.getSleepQuality()).append("\n");
            }
            if (diary.getStressLevel() != null) {
                content.append("压力水平(1-5): ").append(diary.getStressLevel()).append("\n");
            }
            if (StringUtils.hasText(diary.getDiaryContent())) {
                content.append("日记内容: ").append(diary.getDiaryContent());
            }

            EmotionAnalysisDTO analysis = analyzeText(content.toString());
            if (analysis == null) {
                log.warn("情绪分析失败：AI返回内容无法解析, diaryId={}", diaryId);
                return;
            }

            EmotionDiary update = new EmotionDiary();
            update.setId(diaryId);
            update.setAiEmotionAnalysis(JSONUtil.toJsonStr(analysis));
            update.setAiAnalysisUpdatedAt(LocalDateTime.now());
            emotionDiaryMapper.updateById(update);
            log.info("日记情绪分析完成, diaryId={}", diaryId);
        } catch (Exception e) {
            // 异步任务失败仅记录日志，不影响主流程
            log.error("日记情绪分析异常, diaryId={}", diaryId, e);
        }
    }

    /**
     * 调用 AI 分析文本内容，返回结构化情绪分析结果
     *
     * @param content 待分析的对话或日记文本
     * @return 分析结果，解析失败返回 null
     */
    private EmotionAnalysisDTO analyzeText(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String text = content.length() > MAX_ANALYSIS_TEXT_LENGTH
                ? content.substring(content.length() - MAX_ANALYSIS_TEXT_LENGTH)
                : content;

        String aiResponse = analysisChatClient.prompt()
                .system(PromptManage.EMOTION_ANALYSIS_SYSTEM_PROMPT)
                .user(text)
                .call()
                .content();

        return parseAnalysis(aiResponse);
    }

    /**
     * 解析 AI 返回的 JSON 文本为分析结果，并对字段做边界校验
     */
    private EmotionAnalysisDTO parseAnalysis(String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            return null;
        }
        try {
            // 去除可能的 markdown 代码块标记，截取首个 { 到最后一个 } 之间的内容
            String json = aiResponse.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            JSONObject jsonObject = JSONUtil.parseObj(json.substring(start, end + 1));
            EmotionAnalysisDTO analysis = jsonObject.toBean(EmotionAnalysisDTO.class);
            if (analysis == null || !StringUtils.hasText(analysis.getPrimaryEmotion())) {
                return null;
            }
            // 字段边界校验
            if (analysis.getEmotionScore() == null || analysis.getEmotionScore() < 0) {
                analysis.setEmotionScore(50);
            } else if (analysis.getEmotionScore() > 100) {
                analysis.setEmotionScore(100);
            }
            if (analysis.getRiskLevel() == null || analysis.getRiskLevel() < 0) {
                analysis.setRiskLevel(0);
            } else if (analysis.getRiskLevel() > 3) {
                analysis.setRiskLevel(3);
            }
            if (analysis.getIsNegative() == null) {
                analysis.setIsNegative(analysis.getEmotionScore() < 40);
            }
            analysis.setTimestamp(System.currentTimeMillis());
            return analysis;
        } catch (Exception e) {
            log.warn("情绪分析 JSON 解析失败: {}", aiResponse, e);
            return null;
        }
    }
}
