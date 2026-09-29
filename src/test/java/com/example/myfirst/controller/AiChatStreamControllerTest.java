package com.example.myfirst.controller;

import com.example.myfirst.dto.response.AiChatHistoryResponse;
import com.example.myfirst.dto.response.AiChatMessageResponse;
import com.example.myfirst.service.AiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatStreamControllerTest {

    @Mock
    private AiChatService aiChatService;

    @Mock
    private ExecutorService aiChatExecutor;

    @InjectMocks
    private AiChatStreamController controller;

    @Test
    @DisplayName("getChatHistory returns Result with history data")
    void shouldReturnChatHistory() {
        AiChatMessageResponse msg = new AiChatMessageResponse(1L, "user", "Hello", "2026-09-29T10:00:00");
        AiChatHistoryResponse history = new AiChatHistoryResponse(100L, List.of(msg), false);
        when(aiChatService.getChatHistory(1L)).thenReturn(history);

        var result = controller.getChatHistory(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getConversationId()).isEqualTo(100L);
        assertThat(result.getData().getMessages()).hasSize(1);
    }

    @Test
    @DisplayName("archiveConversation returns success")
    void shouldArchiveConversation() {
        var result = controller.archiveConversation(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }
}
