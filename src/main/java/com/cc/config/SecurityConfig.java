package com.cc.config;

import cn.hutool.core.text.AntPathMatcher;
import com.cc.util.JwtAuthticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类，定义公开路径、会话策略与 JWT 过滤器链
 *
 * @author cc
 * @date 2026-09-01
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private  static final String[] PUBLIC_PATHS = {
            "/",
            "/api/user/login",
            "/api/user/add",
            "/api/user/email/send-code",
            "/api/user/email/login",
            "/api/user/email/send-register-code",
            "/api/user/email/register",
            "/api/user/password/reset",
            // 系统设置公开接口：只暴露白名单展示类 key（不含任何开关），前端布局/危机卡片用
            "/api/settings/public",
            "/api/crisis/config",
            // WebSocket 握手端点：浏览器原生 WS 无法携带自定义请求头，由握手拦截器校验 query token
            "/ws/**",
            // MCP 出口（SSE / Streamable HTTP）：外部客户端没有站内 JWT，由 McpApiKeyFilter 校验共享密钥
            "/mcp",
            "/mcp/**",
            "/sse"
    };

    public static Boolean isPublicPATH(String requestUri) {
        for (String publicPath : PUBLIC_PATHS) {
            if (antPathMatcher.match(publicPath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public JwtAuthticationFilter jwtAuthticationFilter() {
        return new JwtAuthticationFilter();
    }

    /**
     * 默认写法
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护 （API服务通常不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置会话管理为无状态（JWT需要）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求的授权规则
                .authorizeHttpRequests(auth -> auth
                        // 仅对初始 REQUEST 分发做鉴权：SSE/Flux 流式接口完成后的 ASYNC 分发不再走鉴权，
                        // 否则异步分发时安全上下文为空会抛出 AccessDenied 且响应已提交，导致
                        // "Unable to handle the Spring Security Exception because the response is already committed"
                        .shouldFilterAllDispatcherTypes(false)
                        // 公开的路径，无需登录即可访问
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // 其他请求都需要认证
                        .anyRequest().authenticated()
                )
                // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
