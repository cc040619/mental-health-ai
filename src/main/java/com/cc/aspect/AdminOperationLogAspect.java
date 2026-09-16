package com.cc.aspect;

import com.cc.util.CurrentUserUtils;
import com.cc.service.OperationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 管理端操作日志切面：拦截 controller 包全部方法，命中 /api/admin/** 且非 GET 时
 * 异步记录操作日志（操作人/动作类型/参数摘要/IP）
 *
 * <p>硬约束：日志采集自身任何异常都吞掉并 log.warn，<b>绝不影响业务接口</b>；
 * 业务异常原样抛出不记录（只记成功执行的写操作）。</p>
 *
 * <p>敏感参数黑名单脱敏：字段名含 password/pwd/token/secret/verify/code/key 的参数值
 * 替换为 ***，防止密码/验证码/密钥落库明文；内容截断 1000 字。</p>
 *
 * <p>登录/登出（/api/user/**）不在 /api/admin 前缀内，由 UserController 显式调用
 * {@link OperationLogService#record} 记录。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Aspect
@Component
public class AdminOperationLogAspect {

    /** 敏感参数字段名黑名单（contains 匹配，命中即脱敏） */
    private static final Set<String> SENSITIVE_FIELD_WORDS =
            Set.of("password", "pwd", "token", "secret", "verify", "code", "key");

    /** 单条日志内容上限（与表字段 varchar(1000) 对齐） */
    private static final int CONTENT_MAX_LENGTH = 1000;

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private ObjectMapper objectMapper;

    @Around("execution(* com.cc.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 业务调用先行：日志绝不前置、绝不阻塞业务
        Object result = joinPoint.proceed();
        try {
            recordIfAdminWrite(joinPoint);
        } catch (Exception e) {
            // 硬约束：日志模块自身异常不影响业务
            log.warn("操作日志记录异常（已忽略）: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 命中 /api/admin/** 且非 GET 才记录；operator 解析失败跳过（未登录写操作不存在于 admin 路径）
     */
    private void recordIfAdminWrite(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        String uri = request.getRequestURI();
        String httpMethod = request.getMethod();
        if (!uri.startsWith("/api/admin") || "GET".equalsIgnoreCase(httpMethod)) {
            return;
        }
        String actionType = switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> httpMethod.toUpperCase();
        };
        Long operatorId = null;
        String operatorName = null;
        try {
            operatorId = CurrentUserUtils.getCurrentUserId();
            operatorName = CurrentUserUtils.getCurrentUsername();
        } catch (Exception e) {
            // 未登录/解析失败：记匿名操作（正常情况下 admin 路径已被鉴权拦截）
        }
        // IP 必须在请求线程解析后传入（@Async 线程读不到 ThreadLocal）
        operationLogService.record(operatorId, operatorName, actionType,
                joinPoint.getSignature().getName() + " " + buildArgsSummary(joinPoint.getArgs()),
                CurrentUserUtils.getCurrentClientIp());
    }

    /**
     * 参数摘要：过滤 Servlet/MultipartFile 对象，敏感字段脱敏，序列化失败降级 toString
     */
    private String buildArgsSummary(Object[] args) {
        List<Object> safeArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                safeArgs.add(null);
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile) {
                safeArgs.add(arg.getClass().getSimpleName());
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(arg));
                maskSensitiveFields(node);
                safeArgs.add(node);
            } catch (Exception e) {
                safeArgs.add(String.valueOf(arg));
            }
        }
        try {
            String json = objectMapper.writeValueAsString(safeArgs);
            return json.length() <= CONTENT_MAX_LENGTH ? json : json.substring(0, CONTENT_MAX_LENGTH);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    /**
     * 递归脱敏：字段名命中黑名单替换 ***，其余递归下钻
     */
    private void maskSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (isSensitiveField(fieldName)) {
                    obj.put(fieldName, "***");
                } else {
                    maskSensitiveFields(obj.get(fieldName));
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::maskSensitiveFields);
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return SENSITIVE_FIELD_WORDS.stream().anyMatch(lower::contains);
    }
}
