package com.example.myfirst.service;

import com.example.myfirst.dto.response.ConversationResponse;
import com.example.myfirst.dto.response.CreateConversationResponse;
import com.example.myfirst.dto.response.UnreadConversationCountResponse;

import java.util.List;

public interface ConversationService {

    List<ConversationResponse> getConversations(Long userId, int page, int size);

    ConversationResponse getConversation(Long conversationId, Long userId);

    CreateConversationResponse createConversation(Long currentUserId, Long otherUserId);

    void markConversationRead(Long conversationId, Long userId);

    UnreadConversationCountResponse getUnreadConversationCount(Long userId);
}
