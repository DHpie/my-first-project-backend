package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.ConversationResponse;
import com.example.myfirst.dto.response.CreateConversationResponse;
import com.example.myfirst.dto.response.UnreadConversationCountResponse;
import com.example.myfirst.entity.Conversation;
import com.example.myfirst.entity.ConversationParticipant;
import com.example.myfirst.entity.Message;
import com.example.myfirst.entity.User;
import com.example.myfirst.repository.ConversationParticipantRepository;
import com.example.myfirst.repository.ConversationRepository;
import com.example.myfirst.repository.MessageRepository;
import com.example.myfirst.repository.UserRepository;
import com.example.myfirst.service.ConversationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId, int page, int size) {
        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId, PageRequest.of(page, size));
        return conversations.stream()
                .map(conv -> toConversationResponse(conv, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId, Long userId) {
        Conversation conv = conversationRepository.findConversationByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found or access denied"));
        return toConversationResponse(conv, userId);
    }

    @Override
    @Transactional
    public CreateConversationResponse createConversation(Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new IllegalArgumentException("Cannot create conversation with yourself");
        }

        // 确保 user1Id < user2Id
        Long user1Id = Math.min(currentUserId, otherUserId);
        Long user2Id = Math.max(currentUserId, otherUserId);

        // 检查是否已有会话
        return conversationRepository.findByUser1IdAndUser2Id(user1Id, user2Id)
                .map(existing -> new CreateConversationResponse(existing.getId()))
                .orElseGet(() -> createNewConversation(user1Id, user2Id));
    }

    @Override
    @Transactional
    public void markConversationRead(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository
                .findByIdConversationIdAndIdUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation participant not found"));
        participant.setUnreadCount(0);
        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadConversationCountResponse getUnreadConversationCount(Long userId) {
        long count = participantRepository.countUnreadConversationsByUserId(userId);
        return new UnreadConversationCountResponse(count);
    }

    private CreateConversationResponse createNewConversation(Long user1Id, Long user2Id) {
        Conversation conversation = new Conversation();
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        Conversation saved = conversationRepository.save(conversation);

        // 创建两个参与者的记录
        ConversationParticipant p1 = new ConversationParticipant(saved.getId(), user1Id);
        ConversationParticipant p2 = new ConversationParticipant(saved.getId(), user2Id);
        participantRepository.save(p1);
        participantRepository.save(p2);

        return new CreateConversationResponse(saved.getId());
    }

    private ConversationResponse toConversationResponse(Conversation conv, Long currentUserId) {
        // 确定对方用户 ID
        Long otherUserId = conv.getUser1Id().equals(currentUserId) ? conv.getUser2Id() : conv.getUser1Id();

        // 获取对方用户信息
        User otherUser = userRepository.findById(otherUserId).orElse(null);
        String otherUserNickname = otherUser != null ? (otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername()) : "Deleted user";
        String otherUserAvatar = otherUser != null ? otherUser.getAvatarUrl() : null;

        // 获取最后一条消息片段
        String lastMessageSnippet = "";
        if (conv.getLastMessageAt() != null) {
            var lastMsgOpt = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conv.getId());
            if (lastMsgOpt.isPresent()) {
                String content = lastMsgOpt.get().getContent();
                lastMessageSnippet = content.length() > 40 ? content.substring(0, 40) : content;
            }
        }

        // 获取当前用户的未读计数
        ConversationParticipant participant = participantRepository
                .findByIdConversationIdAndIdUserId(conv.getId(), currentUserId)
                .orElse(null);
        int unreadCount = participant != null ? participant.getUnreadCount() : 0;

        return new ConversationResponse(
                conv.getId(),
                otherUserId,
                otherUserAvatar,
                otherUserNickname,
                lastMessageSnippet,
                conv.getLastMessageAt(),
                unreadCount
        );
    }
}
