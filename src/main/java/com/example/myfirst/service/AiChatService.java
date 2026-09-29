package com.example.myfirst.service;

import com.example.myfirst.dto.response.AiChatHistoryResponse;
import reactor.core.publisher.Flux;

public interface AiChatService {

    Flux<String> streamMessage(Long userId, String message, Long conversationId);

    AiChatHistoryResponse getChatHistory(Long userId);

    void archiveConversation(Long userId);
}
