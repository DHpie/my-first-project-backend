package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.service.PostInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 帖子互动 API —— 点赞、评论、回复（触发通知生成）
 * <p>
 * 注意：当前项目尚未实现认证机制，userId 通过请求头 X-User-Id 传入。
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostInteractionController {

    private final PostInteractionService postInteractionService;

    /**
     * 点赞帖子
     * POST /api/posts/{postId}/like
     */
    @PostMapping("/{postId}/like")
    public Result<NotificationResponse> likePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        NotificationResponse notification = postInteractionService.likePost(userId, postId);
        return Result.success(notification);
    }

    /**
     * 评论帖子
     * POST /api/posts/{postId}/comment
     */
    @PostMapping("/{postId}/comment")
    public Result<NotificationResponse> commentOnPost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        NotificationResponse notification = postInteractionService.commentOnPost(userId, postId, content);
        return Result.success(notification);
    }

    /**
     * 回复评论
     * POST /api/comments/{commentId}/reply
     */
    @PostMapping("/comments/{commentId}/reply")
    public Result<NotificationResponse> replyToComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body) {
        Long commentOwnerId = Long.parseLong(body.getOrDefault("commentOwnerId", "0"));
        String content = body.getOrDefault("content", "");
        NotificationResponse notification = postInteractionService.replyToComment(userId, commentOwnerId, commentId, content);
        return Result.success(notification);
    }
}
