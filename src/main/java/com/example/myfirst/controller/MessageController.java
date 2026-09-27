package com.example.myfirst.controller;

import com.example.myfirst.common.CurrentUserUtil;
import com.example.myfirst.common.Result;
import com.example.myfirst.dto.request.SendMessageRequest;
import com.example.myfirst.dto.response.MessageResponse;
import com.example.myfirst.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/{id}/messages")
    public Result<MessageResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest sendRequest,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        MessageResponse response = messageService.sendMessage(id, userId, sendRequest.getContent());
        return Result.success(response);
    }

    @GetMapping("/{id}/messages")
    public Result<List<MessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        Long userId = CurrentUserUtil.requireUserId(request);
        List<MessageResponse> responses = messageService.getMessages(id, userId, page, size);
        return Result.success(responses);
    }
}
