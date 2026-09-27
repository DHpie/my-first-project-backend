package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.ConversationResponse;
import com.example.myfirst.dto.response.CreateConversationResponse;
import com.example.myfirst.dto.response.UnreadConversationCountResponse;
import com.example.myfirst.entity.Conversation;
import com.example.myfirst.entity.ConversationParticipant;
import com.example.myfirst.entity.User;
import com.example.myfirst.repository.ConversationParticipantRepository;
import com.example.myfirst.repository.ConversationRepository;
import com.example.myfirst.repository.MessageRepository;
import com.example.myfirst.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    @Nested
    @DisplayName("createConversation")
    class CreateConversation {

        @Test
        @DisplayName("自己对自己 → 400 'Cannot create conversation with yourself'")
        void shouldThrowWhenCreatingConversationWithSelf() {
            assertThatThrownBy(() -> conversationService.createConversation(1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot create conversation with yourself");
        }

        @Test
        @DisplayName("已有会话 → 返回已有会话 ID（不创建重复）")
        void shouldReturnExistingConversationWhenDuplicate() {
            Conversation existing = new Conversation();
            existing.setId(100L);
            existing.setUser1Id(1L);
            existing.setUser2Id(2L);

            when(conversationRepository.findByUser1IdAndUser2Id(1L, 2L))
                    .thenReturn(Optional.of(existing));

            CreateConversationResponse response = conversationService.createConversation(1L, 2L);

            assertThat(response.getConversationId()).isEqualTo(100L);
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("创建新会话 → user1Id < user2Id 排序 + 创建参与者")
        void shouldCreateNewConversationWithSortedIds() {
            when(conversationRepository.findByUser1IdAndUser2Id(2L, 5L))
                    .thenReturn(Optional.empty());

            Conversation saved = new Conversation();
            saved.setId(200L);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

            CreateConversationResponse response = conversationService.createConversation(5L, 2L);

            assertThat(response.getConversationId()).isEqualTo(200L);

            // 验证 user1Id < user2Id
            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).save(captor.capture());
            Conversation captured = captor.getValue();
            assertThat(captured.getUser1Id()).isEqualTo(2L);
            assertThat(captured.getUser2Id()).isEqualTo(5L);

            // 验证创建了两个参与者
            verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
        }
    }

    @Nested
    @DisplayName("markConversationRead")
    class MarkRead {

        @Test
        @DisplayName("标记已读 → unreadCount 设为 0")
        void shouldSetUnreadCountToZero() {
            ConversationParticipant participant = new ConversationParticipant(100L, 1L);
            participant.setUnreadCount(5);

            when(participantRepository.findByIdConversationIdAndIdUserId(100L, 1L))
                    .thenReturn(Optional.of(participant));

            conversationService.markConversationRead(100L, 1L);

            assertThat(participant.getUnreadCount()).isEqualTo(0);
            verify(participantRepository).save(participant);
        }
    }

    @Nested
    @DisplayName("getUnreadConversationCount")
    class UnreadCount {

        @Test
        @DisplayName("返回未读会话计数")
        void shouldReturnUnreadCount() {
            when(participantRepository.countUnreadConversationsByUserId(1L)).thenReturn(3L);

            UnreadConversationCountResponse response = conversationService.getUnreadConversationCount(1L);

            assertThat(response.getCount()).isEqualTo(3L);
        }
    }
}
