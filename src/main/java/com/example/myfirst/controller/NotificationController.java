package com.example.myfirst.controller;

import com.example.myfirst.common.Result;
import com.example.myfirst.dto.response.NotificationListResponse;
import com.example.myfirst.dto.response.UnreadCountResponse;
import com.example.myfirst.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知 REST API 控制器
 * <p>
 * 注意：当前项目尚未实现认证机制，userId 通过请求头 X-User-Id 传入。
 * 后续接入认证后，应从 SecurityContext 中获取当前用户 ID。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     分页获取通知列表
     GET /api/notifications?page=0&size=20
     */
    @GetMapping
    public Result<NotificationListResponse> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        NotificationListResponse response = notificationService.getNotifications(userId, page, size);
        return Result.success(response);
    }

    /**
     标记单条通知已读
     PUT /api/notifications/{uuid}/read
     */
    @PutMapping("/{uuid}/read")
    public Result<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String uuid) {
        notificationService.markAsRead(uuid, userId);
        return Result.success();
    }

    /**
     批量标记全部已读
     PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     获取未读通知计数
     GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public Result<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return Result.success(response);
    }
}
