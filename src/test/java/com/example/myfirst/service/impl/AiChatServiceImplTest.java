package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.AiChatHistoryResponse;
import com.example.myfirst.entity.AiConversation;
import com.example.myfirst.entity.AiMessage;
import com.example.myfirst.repository.AiConversationRepository;
import com.example.myfirst.repository.AiMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock private AiConversationRepository conversationRepository;
    @Mock private AiMessageRepository messageRepository;
    @Mock private ChatModel chatModel;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    @Nested
    @DisplayName("getChatHistory")
    class GetChatHistory {

        @Test
        @DisplayName("no active conversation → returns empty response")
        void shouldReturnEmptyWhenNoConversation() {
            when(conversationRepository.findActiveByUserId(1L)).thenReturn(Optional.empty());

            AiChatHistoryResponse response = aiChatService.getChatHistory(1L);

            assertThat(response.getConversationId()).isNull();
            assertThat(response.getMessages()).isEmpty();
            assertThat(response.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("active conversation with messages → returns messages")
        void shouldReturnMessagesWhenConversationExists() {
            AiConversation conv = new AiConversation();
            conv.setId(100L);
            when(conversationRepository.findActiveByUserId(1L)).thenReturn(Optional.of(conv));

            AiMessage msg1 = new AiMessage();
            msg1.setId(1L);
            msg1.setRole("user");
            msg1.setContent("Hello");
            msg1.setCreatedAt(LocalDateTime.of(2026, 9, 29, 10, 0));

            AiMessage msg2 = new AiMessage();
            msg2.setId(2L);
            msg2.setRole("assistant");
            msg2.setContent("Hi!");
            msg2.setCreatedAt(LocalDateTime.of(2026, 9, 29, 10, 1));

            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L))
                    .thenReturn(List.of(msg1, msg2));
            when(messageRepository.countByConversationId(100L)).thenReturn(2L);

            AiChatHistoryResponse response = aiChatService.getChatHistory(1L);

            assertThat(response.getConversationId()).isEqualTo(100L);
            assertThat(response.getMessages()).hasSize(2);
            assertThat(response.getMessages().get(0).getRole()).isEqualTo("user");
            assertThat(response.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("more than 100 messages → hasMore is true")
        void shouldSetHasMoreWhenOver100Messages() {
            AiConversation conv = new AiConversation();
            conv.setId(200L);
            when(conversationRepository.findActiveByUserId(2L)).thenReturn(Optional.of(conv));
            when(messageRepository.countByConversationId(200L)).thenReturn(150L);
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(200L))
                    .thenReturn(List.of());

            AiChatHistoryResponse response = aiChatService.getChatHistory(2L);

            assertThat(response.isHasMore()).isTrue();
        }
    }

    @Nested
    @DisplayName("archiveConversation")
    class Archive {

        @Test
        @DisplayName("active conversation exists → sets archivedAt")
        void shouldSetArchivedAt() {
            AiConversation conv = new AiConversation();
            conv.setId(100L);
            when(conversationRepository.findActiveByUserId(1L)).thenReturn(Optional.of(conv));

            aiChatService.archiveConversation(1L);

            assertThat(conv.getArchivedAt()).isNotNull();
            verify(conversationRepository).save(conv);
        }

        @Test
        @DisplayName("no active conversation → does nothing")
        void shouldDoNothingWhenNoConversation() {
            when(conversationRepository.findActiveByUserId(1L)).thenReturn(Optional.empty());

            aiChatService.archiveConversation(1L);

            verify(conversationRepository, never()).save(any());
        }
    }
}
