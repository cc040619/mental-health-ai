package com.cc.websocket;

import com.cc.entity.Appointment;
import com.cc.enums.UserType;
import com.cc.service.AppointmentService;
import com.cc.service.TokenService;
import com.cc.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 预约留言室握手拦截器：解析 query 中的 token / appointmentId，
 * 校验 JWT 签名 + Redis 登录白名单 + 预约归属（本人或管理员），任一失败拒绝握手。
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Component
public class AppointmentHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private TokenService tokenService;

    @Resource
    private AppointmentService appointmentService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        try {
            MultiValueParams params = parse(servletRequest);
            if (params.token() == null || params.appointmentId() == null) {
                return false;
            }
            JwtTokenUtil.TokenVerificationResult verification = JwtTokenUtil.validateToken(params.token());
            if (verification == null || !verification.isValid()) {
                return false;
            }
            // 与 HTTP 过滤器一致：Redis 白名单拦截已登出/被顶替的 token
            if (!tokenService.isValidLoginToken(verification.getUserId(), params.token())) {
                return false;
            }
            boolean isAdmin = UserType.ADMIN.getCode().equals(verification.getRoleType());
            // 归属校验：不存在/非本人/非管理员直接抛业务异常 → 拒绝握手
            Appointment appointment = appointmentService.getAppointmentForMember(
                    params.appointmentId(), verification.getUserId(), isAdmin);

            attributes.put("appointmentId", appointment.getId());
            attributes.put("userId", verification.getUserId());
            attributes.put("roleType", verification.getRoleType());
            log.info("WebSocket 握手通过: appointmentId={}, userId={}",
                    appointment.getId(), verification.getUserId());
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手被拒: {}, uri={}", e.getMessage(), request.getURI());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后置处理
    }

    private MultiValueParams parse(ServletServerHttpRequest request) {
        Map<String, String> query = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().toSingleValueMap();
        String token = query.get("token");
        Long appointmentId = null;
        try {
            appointmentId = Long.valueOf(query.get("appointmentId"));
        } catch (NumberFormatException ignored) {
            // 非法预约ID保持 null，归属校验失败拒绝握手
        }
        return new MultiValueParams(token, appointmentId);
    }

    /** query 参数解析结果 */
    private record MultiValueParams(String token, Long appointmentId) {
    }
}
