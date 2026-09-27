package com.example.myfirst.mapper;

import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.entity.Notification;
import com.example.myfirst.entity.NotificationType;
import com.example.myfirst.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationMapper 单元测试")
class NotificationMapperTest {

    @Test
    @DisplayName("toResponse — actor 存在时，正确映射所有字段")
    void toResponse_withActor_shouldMapAllFields() {
        // Arrange
        Notification notification = createNotification(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                1L, 2L, NotificationType.LIKE, "POST", 100L,
                "Amazing Chengdu Guide", false, false);

        User actor = new User();
        actor.setId(2L);
        actor.setUuid(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        actor.setUsername("Sarah Chen");

        // Act
        NotificationResponse response = NotificationMapper.toResponse(notification, actor);

        // Assert
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", response.getId());
        assertEquals("LIKE", response.getType());
        assertEquals("https://i.pravatar.cc/40?u=11111111-2222-3333-4444-555555555555", response.getActorAvatar());
        assertEquals("Sarah Chen", response.getActorNickname());
        assertEquals("liked", response.getActionText());
        assertEquals("Amazing Chengdu Guide", response.getContentSnippet());
        assertEquals(notification.getCreatedAt(), response.getTimestamp());
        assertEquals(false, response.getIsRead());
        assertEquals(false, response.getIsContentDeleted());
        assertEquals(100L, response.getTargetId());
    }

    @Test
    @DisplayName("toResponse — actor 为 null 时，使用默认值")
    void toResponse_withNullActor_shouldUseDefaults() {
        // Arrange
        Notification notification = createNotification(
                UUID.randomUUID(), 1L, 999L, NotificationType.COMMENT,
                "POST", 50L, "Great post!", true, false);

        // Act
        NotificationResponse response = NotificationMapper.toResponse(notification, null);

        // Assert
        assertEquals("https://i.pravatar.cc/40?u=unknown", response.getActorAvatar());
        assertEquals("Unknown User", response.getActorNickname());
        assertEquals("commented on", response.getActionText());
        assertEquals(true, response.getIsRead());
    }

    @Test
    @DisplayName("toResponse — REPLY 类型正确映射 actionText")
    void toResponse_replyType_shouldMapActionText() {
        Notification notification = createNotification(
                UUID.randomUUID(), 1L, 2L, NotificationType.REPLY,
                "COMMENT", 30L, "Thanks for the tip", false, true);

        NotificationResponse response = NotificationMapper.toResponse(notification, null);

        assertEquals("REPLY", response.getType());
        assertEquals("replied to", response.getActionText());
        assertEquals(true, response.getIsContentDeleted());
        assertEquals(30L, response.getTargetId());
    }

    // Helper
    private Notification createNotification(UUID uuid, Long userId, Long actorId,
                                            NotificationType type, String targetType,
                                            Long targetId, String snippet,
                                            Boolean isRead, Boolean isContentDeleted) {
        Notification n = new Notification();
        n.setId(1L);
        // 通过反射设置 uuid（BaseEntity 的 @PrePersist 在 save 时才触发）
        try {
            var field = n.getClass().getSuperclass().getDeclaredField("uuid");
            field.setAccessible(true);
            field.set(n, uuid);
            var createdAtField = n.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(n, LocalDateTime.of(2026, 9, 27, 10, 0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        n.setUserId(userId);
        n.setActorId(actorId);
        n.setType(type);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setContentSnippet(snippet);
        n.setIsRead(isRead);
        n.setIsContentDeleted(isContentDeleted);
        return n;
    }
}
