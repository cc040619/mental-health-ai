package com.cc.aspect;

import com.cc.service.AiCallLogService;
import com.cc.service.AiConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * AI 调用埋点切面：切 AIservice 包全部 public 方法，记 purpose/耗时/成功失败（异步落库）
 *
 * <p>口径：durationMs 为方法调用耗时——非流式调用=总耗时；流式调用（返回 Flux）为
 * 流建立耗时，总耗时不在本次统计（Flux 完成回调与业务耦合复杂），升级路径=Flux doOnFinally 埋点。
 * token/费用尽力提取：AOP 层拿不到响应 usage（调用点均返回 String/Flux），记 NULL；
 * 仅实时测试/试跑接口手动记录 usage（见 AiConfigService.test）。</p>
 *
 * <p>注意：AssessmentService 等非 AIservice 包的 AI 调用点不在此切面统计范围。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Aspect
@Component
public class AiCallLogAspect {

    /** 方法名 → 统计用途映射（未映射记方法名本身） */
    private static final Map<String, String> PURPOSE_MAP = Map.of(
            "streamPsychologicalChat", "STREAM_CHAT",
            "analyzeSessionAsync", "EMOTION_ANALYSIS",
            "analyzeDiaryAsync", "EMOTION_ANALYSIS",
            "generateSummaryAsync", "SUMMARY");

    /** 不统计的方法（未发起 AI 调用） */
    private static final Set<String> SKIP_METHODS = Set.of("startSession");

    @Resource
    private AiCallLogService aiCallLogService;

    @Resource
    private AiConfigService aiConfigService;

    @Around("execution(* com.cc.AIservice..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        if (SKIP_METHODS.contains(method)) {
            return joinPoint.proceed();
        }
        String purpose = PURPOSE_MAP.getOrDefault(method, method);
        // model 从缓存读（60s），取不到记 null；统计取值失败不影响 AI 主流程
        String model = null;
        try {
            model = aiConfigService.getValue(AiConfigService.KEY_MODEL, null);
        } catch (Exception ignored) {
        }
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            int durationMs = (int) (System.currentTimeMillis() - start);
            aiCallLogService.logAsync(purpose, model, null, null, durationMs, 1, null);
            return result;
        } catch (Throwable e) {
            int durationMs = (int) (System.currentTimeMillis() - start);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            aiCallLogService.logAsync(purpose, model, null, null, durationMs, 0,
                    msg.length() > 200 ? msg.substring(0, 200) : msg);
            throw e;
        }
    }
}
