package com.example.myfirst.controller;

import com.example.myfirst.common.CurrentUserUtil;
import com.example.myfirst.common.Result;
import com.example.myfirst.dto.request.CreateConversationRequest;
import com.example.myfirst.dto.response.ConversationResponse;
import com.example.myfirst.dto.response.CreateConversationResponse;
import com.example.myfirst.dto.response.UnreadConversationCountResponse;
import com.example.myfirst.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public Result<List<ConversationResponse>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        List<ConversationResponse> responses = conversationService.getConversations(userId, page, size);
        return Result.success(responses);
    }

    @GetMapping("/{id}")
    public Result<ConversationResponse> getConversation(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        ConversationResponse response = conversationService.getConversation(id, userId);
        return Result.success(response);
    }

    @PostMapping
    public Result<CreateConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest createRequest,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        CreateConversationResponse response = conversationService.createConversation(userId, createRequest.getOtherUserId());
        return Result.success(response);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markConversationRead(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        conversationService.markConversationRead(id, userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<UnreadConversationCountResponse> getUnreadConversationCount(HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        UnreadConversationCountResponse response = conversationService.getUnreadConversationCount(userId);
        return Result.success(response);
    }
}
