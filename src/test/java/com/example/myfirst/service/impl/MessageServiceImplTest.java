package com.example.myfirst.service.impl;

import com.example.myfirst.common.ForbiddenException;
import com.example.myfirst.dto.response.MessageResponse;
import com.example.myfirst.entity.Conversation;
import com.example.myfirst.entity.ConversationParticipant;
import com.example.myfirst.entity.Message;
import com.example.myfirst.repository.ConversationParticipantRepository;
import com.example.myfirst.repository.ConversationRepository;
import com.example.myfirst.repository.MessageRepository;
import com.example.myfirst.repository.UserBlockRepository;
import com.example.myfirst.websocket.MessageWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private UserBlockRepository userBlockRepository;
    @Mock private MessageWebSocketHandler webSocketHandler;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MessageServiceImpl messageService;

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("会话不存在 → 404")
        void shouldThrowWhenConversationNotFound() {
            when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendMessage(999L, 1L, "hello"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Conversation not found");
        }

        @Test
        @DisplayName("非参与者发送 → 404")
        void shouldThrowWhenSenderNotParticipant() {
            Conversation conv = new Conversation();
            conv.setId(100L);
            conv.setUser1Id(1L);
            conv.setUser2Id(2L);
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
            when(participantRepository.findByIdConversationIdAndIdUserId(100L, 3L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendMessage(100L, 3L, "hello"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Conversation not found or access denied");
        }

        @Test
        @DisplayName("被对方屏蔽 → 403 'You have been blocked by this user'")
        void shouldThrowWhenBlockedByOtherUser() {
            Conversation conv = new Conversation();
            conv.setId(100L);
            conv.setUser1Id(1L);
            conv.setUser2Id(2L);
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
            when(participantRepository.findByIdConversationIdAndIdUserId(100L, 1L))
                    .thenReturn(Optional.of(new ConversationParticipant(100L, 1L)));
            // 用户 2 屏蔽了用户 1
            when(userBlockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> messageService.sendMessage(100L, 1L, "hello"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You have been blocked by this user");
        }

        @Test
        @DisplayName("正常发送 → 持久化 + 更新 lastMessageAt + 增加对方 unreadCount + WebSocket 推送")
        void shouldSendMessageSuccessfully() {
            Conversation conv = new Conversation();
            conv.setId(100L);
            conv.setUser1Id(1L);
            conv.setUser2Id(2L);
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
            when(participantRepository.findByIdConversationIdAndIdUserId(100L, 1L))
                    .thenReturn(Optional.of(new ConversationParticipant(100L, 1L)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(false);

            Message savedMsg = new Message();
            savedMsg.setId(1L);
            savedMsg.setConversationId(100L);
            savedMsg.setSenderId(1L);
            savedMsg.setContent("hello");
            // 模拟 BaseEntity 的 createdAt（由 @PrePersist 自动设置）
            try {
                var createdAtField = savedMsg.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(savedMsg, LocalDateTime.now());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

            ConversationParticipant otherParticipant = new ConversationParticipant(100L, 2L);
            otherParticipant.setUnreadCount(0);
            when(participantRepository.findByIdConversationIdAndIdUserId(100L, 2L))
                    .thenReturn(Optional.of(otherParticipant));

            MessageResponse response = messageService.sendMessage(100L, 1L, "hello");

            // 验证消息持久化
            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(msgCaptor.capture());
            assertThat(msgCaptor.getValue().getContent()).isEqualTo("hello");
            assertThat(msgCaptor.getValue().getSenderId()).isEqualTo(1L);

            // 验证响应
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getContent()).isEqualTo("hello");
            assertThat(response.isMine()).isTrue();

            // 验证对方 unreadCount 增加
            assertThat(otherParticipant.getUnreadCount()).isEqualTo(1);
            verify(participantRepository).save(otherParticipant);

            // 验证 WebSocket 推送
            verify(webSocketHandler).sendMessageToUser(eq(2L), anyString());
        }
    }

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("非参与者查询 → 404")
        void shouldThrowWhenNotParticipant() {
            when(conversationRepository.findConversationByIdAndUserId(100L, 3L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getMessages(100L, 3L, 0, 50))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Conversation not found or access denied");
        }
    }
}
