package com.example.myfirst.websocket;

import com.example.myfirst.entity.User;
import com.example.myfirst.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器
 * <p>
 * 负责管理用户 WebSocket 连接，提供实时通知推送能力。
 * 连接通过 URL 参数 token 进行身份验证（当前使用用户 UUID 作为 Token）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    /**
     用户 ID → WebSocket 会话映射（每个用户一条连接）
     */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        // 检查用户是否存在
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("User not found"));
            return;
        }

        // 存储会话（如果已有旧连接，先关闭）
        WebSocketSession oldSession = userSessions.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            oldSession.close(CloseStatus.NORMAL);
        }

        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
            log.info("WebSocket disconnected: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = extractUserId(session);
        log.error("WebSocket transport error: userId={}, error={}", userId, exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    /**
     向指定用户推送通知消息
     @param userId  目标用户 ID
     @param message JSON 格式的通知消息
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("Notification sent to userId={}", userId);
            } catch (IOException e) {
                log.error("Failed to send notification to userId={}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     检查指定用户是否有活跃的 WebSocket 连接
     */
    public boolean hasActiveSession(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     从 WebSocket 会话中提取用户 ID
     通过 URL 参数 token 获取（当前 token 为用户 UUID 字符串）
     */
    private Long extractUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }

        String query = uri.getQuery();
        if (query == null) {
            return null;
        }

        // 解析 token 参数
        String token = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0])) {
                token = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                break;
            }
        }

        if (token == null || token.isBlank()) {
            return null;
        }

        // 当前实现：token 为用户 ID 的字符串形式
        // 后续可替换为 JWT 解析
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            log.warn("Invalid token format: {}", token);
            return null;
        }
    }
}
