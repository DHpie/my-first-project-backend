package com.example.myfirst.mapper;

import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.entity.Notification;
import com.example.myfirst.entity.User;

/**
 * Notification Entity ↔ DTO 手动映射
 */
public class NotificationMapper {

    private NotificationMapper() {
    }

    /**
     将 Notification 实体转换为响应 DTO
     @param notification 通知实体
     @param actor        操作者用户实体（可为 null，表示操作者已注销）
     */
    public static NotificationResponse toResponse(Notification notification, User actor) {
        String actorAvatar = actor != null
                ? "https://i.pravatar.cc/40?u=" + actor.getUuid()
                : "https://i.pravatar.cc/40?u=unknown";
        String actorNickname = actor != null ? actor.getUsername() : "Unknown User";

        return new NotificationResponse(
                notification.getUuid().toString(),
                notification.getType().name(),
                actorAvatar,
                actorNickname,
                notification.getType().getActionText(),
                notification.getContentSnippet(),
                notification.getCreatedAt(),
                notification.getIsRead(),
                notification.getIsContentDeleted(),
                notification.getTargetId()
        );
    }
}
