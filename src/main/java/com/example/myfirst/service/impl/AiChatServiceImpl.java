package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.AiChatHistoryResponse;
import com.example.myfirst.dto.response.AiChatMessageResponse;
import com.example.myfirst.entity.AiConversation;
import com.example.myfirst.entity.AiMessage;
import com.example.myfirst.repository.AiConversationRepository;
import com.example.myfirst.repository.AiMessageRepository;
import com.example.myfirst.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final ChatModel chatModel;

    @Value("${app.ai.system-prompt}")
    private String systemPrompt;

    private static final int CONTEXT_WINDOW = 20;
    private static final int HISTORY_LIMIT = 100;

    @Override
    public Flux<String> streamMessage(Long userId, String message, Long conversationId) {
        // 1. Find or create active conversation
        AiConversation conversation;
        if (conversationId != null) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        } else {
            conversation = conversationRepository.findActiveByUserId(userId)
                    .orElseGet(() -> {
                        AiConversation conv = new AiConversation();
                        conv.setUserId(userId);
                        conv.setTitle("AI Travel Assistant");
                        return conversationRepository.save(conv);
                    });
        }

        final Long convId = conversation.getId();

        // 2. Load context window
        List<AiMessage> historyDesc = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(convId);
        List<AiMessage> history = new ArrayList<>(historyDesc);
        Collections.reverse(history);

        // 3. Build messages for AI model
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (AiMessage histMsg : history) {
            if ("user".equals(histMsg.getRole())) {
                messages.add(new UserMessage(histMsg.getContent()));
            } else if ("assistant".equals(histMsg.getRole())) {
                messages.add(new AssistantMessage(histMsg.getContent()));
            }
        }
        messages.add(new UserMessage(message));

        // 4. Save user message
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(convId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        messageRepository.save(userMsg);

        // 5. Stream response
        Prompt prompt = new Prompt(messages);
        StringBuilder fullReply = new StringBuilder();

        return chatModel.stream(prompt)
                .flatMap(response -> {
                    String content = response.getResult().getOutput().getText();
                    if (content != null) {
                        fullReply.append(content);
                    }
                    return Flux.just(content != null ? content : "");
                })
                .doOnComplete(() -> {
                    AiMessage assistantMsg = new AiMessage();
                    assistantMsg.setConversationId(convId);
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(fullReply.toString());
                    messageRepository.save(assistantMsg);
                })
                .doOnError(e -> log.error("AI streaming error for user {}: {}", userId, e.getMessage()));
    }

    @Override
    @Transactional(readOnly = true)
    public AiChatHistoryResponse getChatHistory(Long userId) {
        return conversationRepository.findActiveByUserId(userId)
                .map(conv -> {
                    long totalCount = messageRepository.countByConversationId(conv.getId());
                    List<AiMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());

                    List<AiMessage> limited = messages.size() > HISTORY_LIMIT
                            ? messages.subList(messages.size() - HISTORY_LIMIT, messages.size())
                            : messages;

                    List<AiChatMessageResponse> responseMessages = limited.stream()
                            .map(m -> new AiChatMessageResponse(
                                    m.getId(),
                                    m.getRole(),
                                    m.getContent(),
                                    m.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            ))
                            .collect(Collectors.toList());

                    return new AiChatHistoryResponse(conv.getId(), responseMessages, totalCount > HISTORY_LIMIT);
                })
                .orElse(new AiChatHistoryResponse(null, List.of(), false));
    }

    @Override
    @Transactional
    public void archiveConversation(Long userId) {
        conversationRepository.findActiveByUserId(userId).ifPresent(conv -> {
            conv.setArchivedAt(LocalDateTime.now());
            conversationRepository.save(conv);
        });
    }
}
