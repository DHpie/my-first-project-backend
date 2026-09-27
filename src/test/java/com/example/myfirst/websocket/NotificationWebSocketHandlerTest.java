package com.example.myfirst.websocket;

import com.example.myfirst.entity.User;
import com.example.myfirst.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationWebSocketHandler 单元测试")
class NotificationWebSocketHandlerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationWebSocketHandler handler;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUuid(UUID.randomUUID());
        user.setUsername("TestUser");
    }

    private WebSocketSession createMockSession(String tokenParam) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        URI uri = new URI("ws://localhost:8080/ws/notifications" +
                (tokenParam != null ? "?token=" + tokenParam : ""));
        lenient().when(session.getUri()).thenReturn(uri);
        lenient().when(session.getId()).thenReturn("session-" + System.nanoTime());
        lenient().when(session.isOpen()).thenReturn(true);
        return session;
    }

    // ==================== 连接建立 ====================

    @Nested
    @DisplayName("afterConnectionEstablished")
    class ConnectionEstablished {

        @Test
        @DisplayName("有效 token（用户 ID）→ 连接建立 + 存入 session")
        void shouldAcceptValidToken() throws Exception {
            WebSocketSession session = createMockSession("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            handler.afterConnectionEstablished(session);

            assertTrue(handler.hasActiveSession(1L));
        }

        @Test
        @DisplayName("无效 token（非数字）→ 关闭连接")
        void shouldRejectInvalidToken() throws Exception {
            WebSocketSession session = createMockSession("invalid-token");

            handler.afterConnectionEstablished(session);

            verify(session).close(argThat(status ->
                    status.equalsCode(CloseStatus.NOT_ACCEPTABLE)));
            assertFalse(handler.hasActiveSession(1L));
        }

        @Test
        @DisplayName("无 token 参数 → 关闭连接")
        void shouldRejectMissingToken() throws Exception {
            WebSocketSession session = createMockSession(null);

            handler.afterConnectionEstablished(session);

            verify(session).close(argThat(status ->
                    status.equalsCode(CloseStatus.NOT_ACCEPTABLE)));
        }

        @Test
        @DisplayName("用户不存在 → 关闭连接")
        void shouldRejectNonExistentUser() throws Exception {
            WebSocketSession session = createMockSession("999");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            handler.afterConnectionEstablished(session);

            verify(session).close(argThat(status ->
                    status.equalsCode(CloseStatus.NOT_ACCEPTABLE)));
        }

        @Test
        @DisplayName("重复连接 → 关闭旧 session，保留新 session")
        void shouldReplaceExistingSession() throws Exception {
            // 第一次连接
            WebSocketSession session1 = createMockSession("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            handler.afterConnectionEstablished(session1);

            // 第二次连接
            WebSocketSession session2 = createMockSession("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            handler.afterConnectionEstablished(session2);

            // 旧 session 应被关闭
            verify(session1).close(CloseStatus.NORMAL);
            // 新 session 应活跃
            assertTrue(handler.hasActiveSession(1L));
        }
    }

    // ==================== 连接关闭 ====================

    @Nested
    @DisplayName("afterConnectionClosed")
    class ConnectionClosed {

        @Test
        @DisplayName("正常关闭 → 从 map 中移除 session")
        void shouldRemoveSessionOnClose() throws Exception {
            // 先建立连接
            WebSocketSession session = createMockSession("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            handler.afterConnectionEstablished(session);
            assertTrue(handler.hasActiveSession(1L));

            // 关闭连接
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
            assertFalse(handler.hasActiveSession(1L));
        }
    }

    // ==================== 消息推送 ====================

    @Nested
    @DisplayName("sendToUser")
    class SendToUser {

        @Test
        @DisplayName("有活跃 session → 发送消息")
        void shouldSendMessageWhenSessionActive() throws Exception {
            WebSocketSession session = createMockSession("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            handler.afterConnectionEstablished(session);

            handler.sendToUser(1L, "{\"type\":\"LIKE\"}");

            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("无活跃 session → 不抛异常，静默跳过")
        void shouldSilentlySkipWhenNoSession() {
            assertDoesNotThrow(() -> handler.sendToUser(999L, "{\"type\":\"LIKE\"}"));
        }
    }

    // ==================== hasActiveSession ====================

    @Test
    @DisplayName("hasActiveSession — 未连接用户返回 false")
    void shouldReturnFalseForUnknownUser() {
        assertFalse(handler.hasActiveSession(999L));
    }
}
