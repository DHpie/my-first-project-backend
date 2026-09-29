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
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("streamMessage — IDOR 防护")
    class StreamMessageIdor {

        @Test
        @DisplayName("访问其他用户的 conversation 时应抛出异常")
        void shouldThrowWhenConversationDoesNotBelongToUser() {
            // 准备：一个属于 userId=99 的 conversation
            AiConversation otherUserConv = new AiConversation();
            otherUserConv.setId(500L);
            otherUserConv.setUserId(99L);

            when(conversationRepository.findById(500L)).thenReturn(Optional.of(otherUserConv));

            // 执行 & 断言：userId=1 的用户尝试访问该 conversation，应抛出异常
            assertThatThrownBy(() ->
                    aiChatService.streamMessage(1L, "hello", 500L)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Conversation not found");
        }

        @Test
        @DisplayName("访问自己的 conversation 时不应因 IDOR 检查而抛出异常")
        void shouldNotThrowWhenConversationBelongsToUser() {
            // 准备：一个属于 userId=1 的 conversation
            AiConversation ownConv = new AiConversation();
            ownConv.setId(600L);
            ownConv.setUserId(1L);

            when(conversationRepository.findById(600L)).thenReturn(Optional.of(ownConv));
            when(messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(600L))
                    .thenReturn(List.of());
            // streamMessage 会继续执行到 chatModel.stream()，这里不 mock 会返回 null 导致 NPE
            // 所以我们只验证 IDOR 检查通过（不抛 IllegalArgumentException）
            // 使用 any(Prompt.class) 避免与 stream(Message...) 重载方法歧义
            lenient().when(chatModel.stream(any(Prompt.class)))
                    .thenThrow(new RuntimeException("expected: chatModel not fully mocked"));

            // 执行：不应抛出 IllegalArgumentException
            try {
                aiChatService.streamMessage(1L, "hello", 600L);
            } catch (IllegalArgumentException e) {
                // 如果是 IDOR 检查导致的异常，则测试失败
                if ("Conversation not found".equals(e.getMessage())) {
                    throw new AssertionError("不应触发 IDOR 检查，conversation 属于当前用户");
                }
            } catch (Exception e) {
                // 其他异常（如 chatModel 未完全 mock）是可以接受的
            }
        }
    }

    @Nested
    @DisplayName("getActiveConversationId")
    class GetActiveConversationId {

        @Test
        @DisplayName("有活跃 conversation 时返回其 ID")
        void shouldReturnIdWhenActiveConversationExists() {
            AiConversation conv = new AiConversation();
            conv.setId(100L);
            when(conversationRepository.findActiveByUserId(1L)).thenReturn(Optional.of(conv));

            Long result = aiChatService.getActiveConversationId(1L);

            assertThat(result).isEqualTo(100L);
        }

        @Test
        @DisplayName("无活跃 conversation 时返回 null")
        void shouldReturnNullWhenNoActiveConversation() {
            when(conversationRepository.findActiveByUserId(1L)).thenReturn(Optional.empty());

            Long result = aiChatService.getActiveConversationId(1L);

            assertThat(result).isNull();
        }
    }

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
