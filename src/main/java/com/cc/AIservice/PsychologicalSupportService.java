package com.cc.AIservice;

import com.cc.dto.request.ConsultationSessionCreateDTO;
import com.cc.dto.response.ConsultationMessageResponseDTO;
import com.cc.entity.ConsultationSession;
import com.cc.service.AiConfigService;
import com.cc.service.ConsultationMessageService;
import com.cc.service.ConsultationSessionService;
import com.cc.service.CrisisPlanService;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class PsychologicalSupportService {

    /**
     * ponytail: 危机标记前缀（AI 回复不会以 \u0001 开头），Controller 识别后映射为 SSE event("crisis")
     * <p>用不可见控制字符做前缀，保证不污染正常对话内容；前端收到 crisis 事件弹卡片，不等异步情绪分析。
     */
    public static final String CRISIS_MARKER = "\u0001__CRISIS__:";

    /** 命中危机词时追加到 system prompt 前的响应指令（语气谨慎、优先引导热线、不追问细节） */
    private static final String CRISIS_SYSTEM_INSTRUCTION =
            "【紧急】用户可能处于心理危机状态。请先表达关心与支持，优先引导拨打全国心理援助热线 12356，" +
            "语气温和克制，不评判、不追问自伤细节，陪伴用户梳理当下安全。";

    @Resource
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private ConsultationSessionService consultationSessionService;

    @Resource
    private ConsultationMessageService consultationMessageService;

    @Resource
    private EmotionAnalysisService emotionAnalysisService;

    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    @Resource
    private CrisisPlanService crisisPlanService;

    @Resource
    private AiConfigService aiConfigService;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

        // 将初始用户消息保存到Message表
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);

        // 创建会话信息
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, // 24小时
                1,
                "ACTIVE"
        );
    }

    // 流式对话
    public Flux<String> streamPsychologicalChat(Long userId, boolean isAdmin, String sessionId, String userMessage) {
        // 创建响应流
        return Flux.create(sink -> {
            Long dbSessionId = extractSessionId(sessionId);
            if (dbSessionId == null) {
                sink.error(new RuntimeException("会话ID格式错误"));
                return;
            }
            // 校验会话归属；若会话已被删除（前端页面仍持有旧引用）则按原ID重建，保证对话无缝继续
            ConsultationSession session;
            try {
                session = consultationSessionService.getSessionOrRebuild(dbSessionId, userId, isAdmin);
            } catch (Exception e) {
                sink.error(e);
                return;
            }
            // 是否为初始消息
            boolean isInitialMessage = false;
            // 检查是否为初始消息，避免重复保存
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }
            }
            if (!isInitialMessage) {
                // 保存用户消息到数据库
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
            }

            // 进行流式对话
            // 生成对话记忆管理
            String conversationId = "conversation_" + sessionId;
            // 危机检测：复用 SensitiveWordFilter DFA 内存词树（crisis-words.txt），O(n) 扫描毫秒级，不查库不调 AI
            // ponytail: 不新建 CrisisDetector——SensitiveWordFilter.findCrisisWord 即"危机检测一处实现"，社区/AI对话/测评共用
            String crisisHit = sensitiveWordFilter.findCrisisWord(userMessage);
            // 构建系统提示词：按会话场景选择对应提示词（仅内存常量选择，零额外耗时，不引入串行AI调用）
            String systemPrompt = PromptManage.getSystemPromptByScene(session != null ? session.getScene() : null);
            if (crisisHit != null) {
                // 命中危机词：① @Async 记录事件（不阻塞流）② 流最前面推 crisis 标记（Controller 映射为 event("crisis")）
                // ③ 追加 system 危机响应指令（AI 语气更谨慎、优先引导热线）
                crisisPlanService.recordCrisisEvent(userId, CrisisPlanService.SOURCE_CHAT, crisisHit);
                sink.next(CRISIS_MARKER + crisisHit);
                systemPrompt = CRISIS_SYSTEM_INSTRUCTION + systemPrompt;
            }
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt)
            ));

            //用于存储AI完成的响应
            StringBuilder fullResponse = new StringBuilder();

            // 使用chatClient发送消息到Open AI
            // 注意：MessageChatMemoryAdvisor 会在调用前读取记忆并在调用后自动保存本轮用户消息与AI回复，
            // 此处不再手动写入 chatMemory，避免每条消息在记忆中重复导致 AI 回复复读
            chatClient.prompt(prompt)
                    // per-request 模型参数覆盖（DB 配置 60s 缓存），改配置后下一条对话生效
                    .options(aiConfigService.buildChatOptions())
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(Fragment -> {
                        fullResponse.append(Fragment);
                        sink.next(Fragment);
                    })
                    .doOnComplete(() -> {
                        String completeRes = fullResponse.toString();
                        // 将AI返回的内容保存到数据库
                        consultationMessageService.saveAimessage(dbSessionId, completeRes, "openai");

                        // 异步触发会话情绪分析
                        emotionAnalysisService.analyzeSessionAsync(dbSessionId);

                        sink.complete();
                    })
                    .doOnError(error -> {
                        sink.error(error);
                    })
                    .subscribe(); // 订阅并启动流
        });
    }

    // 获取参数中的sessionId
    public Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            // 截取id具体值
            String idStr = sessionId.substring("session_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }
}
