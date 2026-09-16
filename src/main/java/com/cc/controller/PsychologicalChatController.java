package com.cc.controller;

import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cc.AIservice.PsychologicalSupportService;
import com.cc.AIservice.StructOutPut;
import com.cc.common.Result;
import com.cc.common.ResultCode;
import com.cc.dto.request.ConsultationSessionCreateDTO;
import com.cc.dto.request.ConsultationStreamDTO;
import com.cc.dto.request.SessionFeedbackDTO;
import com.cc.dto.request.SessionPageQueryDTO;
import com.cc.dto.response.ConsultationMessageResponseDTO;
import com.cc.dto.response.ConsultationSessionResponseDTO;
import com.cc.dto.response.EmotionAnalysisDTO;
import com.cc.dto.response.FavoriteMessageResponseDTO;
import com.cc.dto.response.HumanHandoffResponseDTO;
import com.cc.dto.response.SessionSummaryResponseDTO;
import com.cc.entity.ConsultationSession;
import com.cc.service.ConsultationMessageService;
import com.cc.service.ConsultationSessionService;
import com.cc.util.CurrentUserUtils;
import com.cc.util.JwtTokenUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChatController {

    @Resource
    private PsychologicalSupportService psychologicalSupportService;

    @Resource
    private ConsultationSessionService consultationSessionService;

    @Resource
    private ConsultationMessageService consultationMessageService;

    // 开始咨询会话接口
    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(), "用户未登录")))
                    .build());
        }

        // 开始流式对话（AI 调用异常时降级为 SSE 错误事件，保证响应始终为 text/event-stream）
        return psychologicalSupportService.streamPsychologicalChat(userId, CurrentUserUtils.isAdmin(), streamDTO.getSessionId(), streamDTO.getUserMessage())
                .map(Fragment -> {
                    // 危机标记识别：命中危机词时 service 在流最前面推 CRISIS_MARKER+keyword，映射为 event("crisis")
                    // 前端收到 crisis 事件立即弹 CrisisSupportCard（dialog 形态），不等异步情绪分析
                    if (Fragment.startsWith(PsychologicalSupportService.CRISIS_MARKER)) {
                        String keyword = Fragment.substring(PsychologicalSupportService.CRISIS_MARKER.length());
                        return ServerSentEvent.<String>builder()
                                .event("crisis")
                                .data(JSONUtil.toJsonStr(Map.of("type", "crisis", "keyword", keyword)))
                                .build();
                    }
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(JSONUtil.toJsonStr(Result.ok(Map.of("content", Fragment, "type", "normal"))))
                            .build();
                })
                .onErrorResume(ex -> {
                    String errorMsg;
                    if (ex instanceof com.cc.exception.BusinessException businessException) {
                        // 业务异常（无权访问等预期内错误）降级为 WARN，不打堆栈
                        log.warn("流式对话业务异常, sessionId={}, 原因: {}", streamDTO.getSessionId(), businessException.getMessage());
                        errorMsg = businessException.getMessage();
                    } else if (ex.getMessage() != null && ex.getMessage().contains("401")) {
                        log.error("流式对话AI认证失败, sessionId={}", streamDTO.getSessionId(), ex);
                        errorMsg = "AI服务认证失败，请联系管理员检查API Key配置";
                    } else {
                        log.error("流式对话异常, sessionId={}", streamDTO.getSessionId(), ex);
                        errorMsg = "AI服务暂时不可用，请稍后重试";
                    }
                    String errorJson = JSONUtil.toJsonStr(Map.of("code", ResultCode.ERROR.getCode(), "message", errorMsg));
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("message")
                            .data(errorJson)
                            .build());
                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()
                ))
                .delayElements(Duration.ofMillis(50)); // 添加延迟确保流式数据的体验
    }

    // 获取会话列表（管理员返回全部会话，普通用户返回自己的会话）
    @GetMapping("/sessions")
    public Result<Map<String, Object>> listSessions(@Valid SessionPageQueryDTO queryDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Page<ConsultationSessionResponseDTO> page = consultationSessionService.pageSessions(
                userId, CurrentUserUtils.isAdmin(), queryDTO.resolvePageNum(), queryDTO.resolvePageSize());
        return Result.ok(Map.of("records", page.getRecords(), "total", page.getTotal()));
    }

    // 删除会话
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        consultationSessionService.deleteSession(sessionId, userId, CurrentUserUtils.isAdmin());
        return Result.ok();
    }

    // 获取会话消息列表
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> listMessages(@PathVariable Long sessionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        ConsultationSession session = consultationSessionService.getSessionOwned(sessionId, userId, CurrentUserUtils.isAdmin());
        return Result.ok(consultationMessageService.listBySessionId(session.getId()));
    }

    // 获取会话情绪分析结果（路径参数为 session_<id> 格式），校验会话归属
    @GetMapping("/session/{sessionId}/emotion")
    public Result<EmotionAnalysisDTO> getSessionEmotion(@PathVariable String sessionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        EmotionAnalysisDTO emotion = consultationSessionService.getSessionEmotion(
                sessionId, userId, CurrentUserUtils.isAdmin());
        return Result.ok(emotion);
    }

    // 触发会话总结：已有总结直接返回，否则异步生成并立即返回"生成中"
    @PostMapping("/sessions/{sessionId}/summary")
    public Result<SessionSummaryResponseDTO> triggerSummary(@PathVariable Long sessionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        SessionSummaryResponseDTO summary = consultationSessionService.triggerSummary(
                sessionId, userId, CurrentUserUtils.isAdmin());
        return Result.ok(summary);
    }

    // 获取会话总结（无总结时返回"生成中"状态）
    @GetMapping("/sessions/{sessionId}/summary")
    public Result<SessionSummaryResponseDTO> getSummary(@PathVariable Long sessionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        SessionSummaryResponseDTO summary = consultationSessionService.getSummary(
                sessionId, userId, CurrentUserUtils.isAdmin());
        return Result.ok(summary);
    }

    // 收藏/取消收藏消息（toggle，返回最新收藏状态）
    @PostMapping("/messages/{messageId}/favorite")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long messageId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Boolean isFavorite = consultationMessageService.toggleFavorite(
                messageId, userId, CurrentUserUtils.isAdmin());
        Map<String, Object> data = new HashMap<>();
        data.put("isFavorite", isFavorite);
        return Result.ok(data);
    }

    // 收藏的AI回复分页列表（size 上限 50）
    @GetMapping("/favorites")
    public Result<Map<String, Object>> listFavorites(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 50);
        Page<FavoriteMessageResponseDTO> page = consultationMessageService.pageFavorites(
                userId, safePageNum, safePageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        return Result.ok(data);
    }

    // 提交会话评价（评分1-5 + 文字反馈，重复提交覆盖更新）
    @PutMapping("/sessions/{sessionId}/feedback")
    public Result<Void> submitFeedback(@PathVariable Long sessionId,
                                       @Valid @RequestBody SessionFeedbackDTO feedbackDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        consultationSessionService.saveFeedback(sessionId, userId, CurrentUserUtils.isAdmin(), feedbackDTO);
        return Result.ok();
    }

    // 转人工引导（模板实现：咨询师模块未上线，返回温和引导文案与求助资源）
    @PostMapping("/sessions/{sessionId}/human-handoff")
    public Result<HumanHandoffResponseDTO> humanHandoff(@PathVariable Long sessionId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        HumanHandoffResponseDTO handoff = consultationSessionService.requestHumanHandoff(
                sessionId, userId, CurrentUserUtils.isAdmin());
        return Result.ok(handoff);
    }
}
