package com.example.myfirst.controller;

import com.example.myfirst.common.CurrentUserUtil;
import com.example.myfirst.common.Result;
import com.example.myfirst.dto.request.AiChatStreamRequest;
import com.example.myfirst.dto.response.AiChatHistoryResponse;
import com.example.myfirst.service.AiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatStreamController {

    private final AiChatService aiChatService;
    private final ExecutorService aiChatExecutor;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @Valid @RequestBody AiChatStreamRequest request,
            HttpServletRequest httpRequest) {
        Long userId = CurrentUserUtil.requireUserId(httpRequest);
        SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout

        aiChatExecutor.execute(() -> {
            try {
                aiChatService.streamMessage(userId, request.getMessage(), request.getConversationId())
                        .doOnNext(token -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .data("{\"content\":\"" + escapeJson(token) + "\"}"));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                Long convId = aiChatService.getActiveConversationId(userId);
                                emitter.send(SseEmitter.event()
                                        .data("{\"content\":\"\",\"done\":true,\"conversationId\":" + convId + "}"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(error -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .data("{\"error\":\"AI service unavailable. Please try again.\"}"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .subscribe();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE error for user {}: {}", userId, e.getMessage()));

        return emitter;
    }

    @GetMapping("/history")
    public Result<AiChatHistoryResponse> getChatHistory(HttpServletRequest request) {
        return getChatHistory(CurrentUserUtil.requireUserId(request));
    }

    @DeleteMapping("/conversation")
    public Result<Void> archiveConversation(HttpServletRequest request) {
        return archiveConversation(CurrentUserUtil.requireUserId(request));
    }

    // Package-private testable methods

    Result<AiChatHistoryResponse> getChatHistory(Long userId) {
        return Result.success(aiChatService.getChatHistory(userId));
    }

    Result<Void> archiveConversation(Long userId) {
        aiChatService.archiveConversation(userId);
        return Result.success();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
