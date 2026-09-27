package com.example.myfirst.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    /** userId -> WebSocketSession 映射 */
    private final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
            log.info("WebSocket connected: userId={}", userId);
        } else {
            log.warn("WebSocket connection rejected: missing userId parameter");
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException e) {
                log.error("Failed to close WebSocket session", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = extractUserId(session);
        log.error("WebSocket transport error: userId={}", userId, exception);
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.error("Failed to close WebSocket session after transport error", e);
            }
        }
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    /**
     * 向指定用户推送消息
     *
     * @param userId  目标用户 ID
     * @param payload JSON 消息内容
     */
    public void sendMessageToUser(Long userId, String payload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
                log.debug("WebSocket message sent to userId={}", userId);
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to userId={}", userId, e);
            }
        } else {
            log.debug("No active WebSocket session for userId={}", userId);
        }
    }

    /**
     * 从 WebSocket URI 查询参数中提取 userId
     * 连接地址格式: ws://host/ws/messages?userId=123
     */
    private Long extractUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String query = uri.getQuery();
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if ("userId".equals(pair[0]) && pair.length == 2) {
                try {
                    return Long.parseLong(pair[1]);
                } catch (NumberFormatException e) {
                    log.warn("Invalid userId parameter: {}", pair[1]);
                    return null;
                }
            }
        }
        return null;
    }
}
