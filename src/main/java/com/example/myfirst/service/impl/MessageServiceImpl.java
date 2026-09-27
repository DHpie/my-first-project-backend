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
import com.example.myfirst.service.MessageService;
import com.example.myfirst.websocket.MessageWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserBlockRepository userBlockRepository;
    private final MessageWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, String content) {
        // 校验会话存在性和参与权限
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        boolean isParticipant = participantRepository
                .findByIdConversationIdAndIdUserId(conversationId, senderId)
                .isPresent();
        if (!isParticipant) {
            throw new EntityNotFoundException("Conversation not found or access denied");
        }

        // 校验是否被对方屏蔽
        Long otherUserId = conversation.getUser1Id().equals(senderId)
                ? conversation.getUser2Id() : conversation.getUser1Id();
        if (userBlockRepository.existsByBlockerIdAndBlockedId(otherUserId, senderId)) {
            throw new ForbiddenException("You have been blocked by this user");
        }

        // 持久化消息
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(content);
        Message saved = messageRepository.save(message);

        // 更新会话 lastMessageAt
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);

        // 增加对方 unreadCount
        participantRepository.findByIdConversationIdAndIdUserId(conversationId, otherUserId)
                .ifPresent(participant -> {
                    participant.setUnreadCount(participant.getUnreadCount() + 1);
                    participantRepository.save(participant);
                });

        // WebSocket 推送给接收方
        pushMessageToUser(otherUserId, saved);

        return toMessageResponse(saved, senderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, Long currentUserId, int page, int size) {
        // 校验会话存在性和参与权限
        conversationRepository.findConversationByIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found or access denied"));

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(page, size))
                .getContent().stream()
                .map(msg -> toMessageResponse(msg, currentUserId))
                .collect(Collectors.toList());
    }

    private void pushMessageToUser(Long userId, Message message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversationId", message.getConversationId());
            payload.put("messageId", message.getId());
            payload.put("content", message.getContent());
            payload.put("senderId", message.getSenderId());
            payload.put("timestamp", message.getCreatedAt().toString());
            String json = objectMapper.writeValueAsString(payload);
            webSocketHandler.sendMessageToUser(userId, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WebSocket message for userId={}", userId, e);
        }
    }

    private MessageResponse toMessageResponse(Message message, Long currentUserId) {
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getSenderId().equals(currentUserId)
        );
    }
}
