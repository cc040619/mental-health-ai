package com.cc.websocket;

import com.cc.dto.response.AppointmentMessageResponseDTO;
import com.cc.entity.Appointment;
import com.cc.enums.UserType;
import com.cc.service.AppointmentMessageService;
import com.cc.service.AppointmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预约留言室处理器：按 appointmentId 维护在线会话房间，
 * 消息校验归属后落库并广播给同预约所有在线会话（用户 senderType=1 / 管理员=2）。
 *
 * <p>安全限制：消息 ≤1000 字；同一会话两条消息间隔不少于 1 秒，超限丢弃并回 SYSTEM 提示。</p>
 *
 * <p>ponytail: 单实例内存房间，水平扩容时升级 Redis Pub/Sub 广播。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Component
public class AppointmentChatHandler extends TextWebSocketHandler {

    /** 留言房间：appointmentId → 在线会话集合 */
    private static final Map<Long, Set<WebSocketSession>> ROOMS = new ConcurrentHashMap<>();

    /** 频率限制：sessionId → 上次发言毫秒时间戳 */
    private static final Map<String, Long> LAST_MESSAGE_AT = new ConcurrentHashMap<>();

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final long MIN_MESSAGE_INTERVAL_MS = 1000L;

    /** 发送缓冲：2 秒超时、64KB 缓冲区，防止慢客户端阻塞广播线程 */
    private static final int SEND_TIME_LIMIT_MS = 2000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

    @Resource
    private AppointmentMessageService appointmentMessageService;
    @Resource
    private AppointmentService appointmentService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long appointmentId = appointmentId(session);
        if (appointmentId == null) {
            return;
        }
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        ROOMS.computeIfAbsent(appointmentId, k -> ConcurrentHashMap.newKeySet()).add(safeSession);
        log.info("留言室加入会话: appointmentId={}, online={}",
                appointmentId, ROOMS.get(appointmentId).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long appointmentId = appointmentId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        Integer roleType = (Integer) session.getAttributes().get("roleType");
        if (appointmentId == null || userId == null || roleType == null) {
            return;
        }

        String content = extractContent(message.getPayload());
        if (content == null || content.isBlank()) {
            sendSystem(session, "消息内容不能为空");
            return;
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            sendSystem(session, "消息内容不能超过1000字");
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_MESSAGE_AT.getOrDefault(session.getId(), 0L);
        if (now - last < MIN_MESSAGE_INTERVAL_MS) {
            sendSystem(session, "说话太快啦，歇一秒再发～");
            return;
        }
        LAST_MESSAGE_AT.put(session.getId(), now);

        boolean isAdmin = UserType.ADMIN.getCode().equals(roleType);
        int senderType = isAdmin ? 2 : 1;
        // 归属二次校验（握手后预约状态/身份不影响留言资格，但拒绝伪造会话）
        Appointment appointment = appointmentService.getAppointmentForMember(appointmentId, userId, isAdmin);

        AppointmentMessageResponseDTO saved = appointmentMessageService.save(appointment, userId, senderType, content);
        broadcast(appointmentId, buildMessageJson(saved));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long appointmentId = appointmentId(session);
        if (appointmentId == null) {
            return;
        }
        Set<WebSocketSession> room = ROOMS.get(appointmentId);
        if (room != null) {
            // 装饰器包装后 id 与原会话一致，按 id 移除
            room.removeIf(s -> s.getId().equals(session.getId()));
            if (room.isEmpty()) {
                ROOMS.remove(appointmentId);
            }
        }
        LAST_MESSAGE_AT.remove(session.getId());
        log.info("留言室离开会话: appointmentId={}, closeStatus={}", appointmentId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("留言室连接异常: sessionId={}, err={}", session.getId(), exception.getMessage());
    }

    // ==================== 内部方法 ====================

    /** 广播给同预约所有在线会话（含发送者本人，作为发送成功回执） */
    private void broadcast(Long appointmentId, String payload) {
        Set<WebSocketSession> room = ROOMS.get(appointmentId);
        if (room == null) {
            return;
        }
        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : room) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (Exception e) {
                    log.warn("留言广播失败: sessionId={}, err={}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /** 单条 SYSTEM 提示（如频率限制） */
    private void sendSystem(WebSocketSession session, String text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(buildSystemJson(text)));
        } catch (Exception e) {
            log.warn("SYSTEM 提示发送失败: sessionId={}, err={}", session.getId(), e.getMessage());
        }
    }

    private String buildMessageJson(AppointmentMessageResponseDTO m) {
        try {
            return objectMapper.createObjectNode()
                    .put("type", "MESSAGE")
                    .put("id", m.getId())
                    .put("senderType", m.getSenderType())
                    .put("content", m.getContent())
                    .put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                    .toString();
        } catch (Exception e) {
            throw new IllegalStateException("留言序列化失败", e);
        }
    }

    private String buildSystemJson(String text) {
        try {
            return objectMapper.createObjectNode()
                    .put("type", "SYSTEM")
                    .put("content", text)
                    .toString();
        } catch (Exception e) {
            return "{\"type\":\"SYSTEM\",\"content\":\"系统繁忙\"}";
        }
    }

    /** 解析入站 JSON：{content: "..."}；非法 JSON 返回 null */
    private String extractContent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode content = node.get("content");
            return content != null && !content.isNull() ? content.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long appointmentId(WebSocketSession session) {
        Object value = session.getAttributes().get("appointmentId");
        return value instanceof Long ? (Long) value : null;
    }
}
