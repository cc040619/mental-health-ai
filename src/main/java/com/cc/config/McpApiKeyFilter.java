package com.cc.config;

import cn.hutool.json.JSONUtil;
import com.cc.common.RedisConstants;
import com.cc.common.Result;
import com.cc.common.ResultCode;
import com.cc.util.ResponseUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;

/**
 * MCP 出口鉴权与限频过滤器
 *
 * <p>MCP 端点不适用站内 JWT（外部客户端没有登录态），改为共享密钥：请求头
 * {@code Authorization: Bearer <MCP_API_KEY>}，密钥由环境变量注入（{@code mcp.api-key}）。
 * 未配置密钥时<b>拒绝全部 MCP 请求</b>（安全默认：配置缺失不等于放行）。</p>
 *
 * <p>密钥比较使用 {@link MessageDigest#isEqual} 定长比较，避免逐字符短路造成的时间侧信道；
 * 限频为 Redis 60 秒固定窗口，Redis 异常时 fail-open（缓存故障不应阻断只读检索）。</p>
 *
 * <p>SSE 与 Streamable HTTP 两种传输的路径都覆盖，切换 {@code spring.ai.mcp.server.protocol} 无需改这里。</p>
 *
 * <p>ponytail: 固定窗口限频，单 Key 粒度；密钥轮换/多租户接入后改成滑动窗口 + 按 Key 计数。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@Slf4j
@Component
public class McpApiKeyFilter extends OncePerRequestFilter {

    /** 需要校验密钥的 MCP 端点前缀（/sse、/mcp/message、/mcp 都命中） */
    private static final List<String> MCP_PATH_PREFIXES = List.of("/mcp", "/sse");
    private static final String BEARER_PREFIX = "Bearer ";
    /** 限频窗口（秒） */
    private static final long RATE_WINDOW_SECONDS = 60L;

    /** MCP 共享密钥，未配置（空）时禁用 MCP 出口 */
    @Value("${mcp.api-key:}")
    private String apiKey;

    /** 每个限频窗口允许的请求数，≤0 表示不限频 */
    @Value("${mcp.rate-limit-per-minute:60}")
    private int rateLimitPerMinute;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return MCP_PATH_PREFIXES.stream().noneMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("MCP 请求被拒：未配置 mcp.api-key, uri={}", request.getRequestURI());
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }
        if (!isValidKey(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            log.warn("MCP 请求被拒：API Key 无效, uri={}", request.getRequestURI());
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }
        if (!allowRequest()) {
            log.warn("MCP 请求被拒：超过限频阈值, uri={}", request.getRequestURI());
            writeTooManyRequests(response);
            return;
        }
        chain.doFilter(request, response);
    }

    /** 定长比较校验 Bearer 密钥，不打印任何密钥内容 */
    private boolean isValidKey(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        String presented = authorization.substring(BEARER_PREFIX.length()).trim();
        return MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }

    /** 60 秒固定窗口计数：Redis 异常时放行（只读检索，可用性优先） */
    private boolean allowRequest() {
        if (rateLimitPerMinute <= 0) {
            return true;
        }
        try {
            Long count = stringRedisTemplate.opsForValue().increment(RedisConstants.MCP_RATE_LIMIT_KEY);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(RedisConstants.MCP_RATE_LIMIT_KEY, Duration.ofSeconds(RATE_WINDOW_SECONDS));
            }
            return count == null || count <= rateLimitPerMinute;
        } catch (Exception e) {
            log.warn("MCP 限频统计失败，放行本次请求: {}", e.getMessage());
            return true;
        }
    }

    private static void writeTooManyRequests(HttpServletResponse response) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.print(JSONUtil.toJsonStr(
                    Result.error(ResultCode.ACCESS_UNAUTHORIZED.getCode(), "请求过于频繁，请稍后重试", null)));
            writer.flush();
        } catch (IOException e) {
            log.warn("MCP 限频响应写入失败: {}", e.getMessage());
        }
    }
}