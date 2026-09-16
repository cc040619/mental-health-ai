package com.cc.config;

import com.cc.websocket.AppointmentChatHandler;
import com.cc.websocket.AppointmentHandshakeInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：注册预约留言室端点 /ws/appointment。
 * 不走 Spring Security 过滤器链（浏览器原生 WebSocket 无法自定义请求头），
 * 由 {@link AppointmentHandshakeInterceptor} 完成 query token 鉴权与预约归属校验。
 *
 * @author cc
 * @date 2026-09-14
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private AppointmentChatHandler appointmentChatHandler;

    @Resource
    private AppointmentHandshakeInterceptor appointmentHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(appointmentChatHandler, "/ws/appointment")
                .addInterceptors(appointmentHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
