package com.example.myfirst.service;

import com.example.myfirst.dto.response.MessageResponse;

import java.util.List;

public interface MessageService {

    MessageResponse sendMessage(Long conversationId, Long senderId, String content);

    List<MessageResponse> getMessages(Long conversationId, Long currentUserId, int page, int size);
}
