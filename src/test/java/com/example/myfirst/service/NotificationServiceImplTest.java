package com.example.myfirst.service;

import com.example.myfirst.common.ForbiddenException;
import com.example.myfirst.dto.response.NotificationListResponse;
import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.dto.response.UnreadCountResponse;
import com.example.myfirst.entity.Notification;
import com.example.myfirst.entity.NotificationType;
import com.example.myfirst.entity.User;
import com.example.myfirst.mapper.NotificationMapper;
import com.example.myfirst.repository.NotificationRepository;
import com.example.myfirst.repository.UserRepository;
import com.example.myfirst.service.impl.NotificationServiceImpl;
import com.example.myfirst.websocket.NotificationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 单元测试")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationWebSocketHandler webSocketHandler;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User actor;
    private Notification sampleNotification;
    private UUID notificationUuid;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(2L);
        actor.setUuid(UUID.randomUUID());
        actor.setUsername("Sarah");

        notificationUuid = UUID.randomUUID();
        sampleNotification = new Notification();
        sampleNotification.setId(1L);
        sampleNotification.setUserId(1L);
        sampleNotification.setActorId(2L);
        sampleNotification.setType(NotificationType.LIKE);
        sampleNotification.setTargetType("POST");
        sampleNotification.setTargetId(100L);
        sampleNotification.setContentSnippet("Test snippet");
        sampleNotification.setIsRead(false);
        sampleNotification.setIsContentDeleted(false);
        // 设置 uuid 和 createdAt
        try {
            var uuidField = sampleNotification.getClass().getSuperclass().getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(sampleNotification, notificationUuid);
            var createdAtField = sampleNotification.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(sampleNotification, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== createNotification ====================

    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Test
        @DisplayName("正常创建 — 不同用户操作 → 返回通知 + WebSocket 推送")
        void shouldCreateNotificationAndPush() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
            when(webSocketHandler.hasActiveSession(1L)).thenReturn(true);

            NotificationResponse result = notificationService.createNotification(
                    1L, 2L, NotificationType.LIKE, "POST", 100L, "Test snippet");

            assertNotNull(result);
            assertEquals("LIKE", result.getType());
            verify(notificationRepository).save(any(Notification.class));
            verify(webSocketHandler).hasActiveSession(1L);
            verify(webSocketHandler).sendToUser(eq(1L), anyString());
        }

        @Test
        @DisplayName("自己操作自己 → 返回 null，不保存")
        void shouldReturnNullWhenSelfAction() {
            NotificationResponse result = notificationService.createNotification(
                    1L, 1L, NotificationType.LIKE, "POST", 100L, "Test");

            assertNull(result);
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("重复点赞去重 — 未读通知存在时先删除再生成")
        void shouldDeleteExistingUnreadLikeBeforeCreating() {
            Notification existingUnread = new Notification();
            existingUnread.setId(99L);
            existingUnread.setUserId(1L);
            existingUnread.setActorId(2L);
            existingUnread.setType(NotificationType.LIKE);
            existingUnread.setTargetType("POST");
            existingUnread.setTargetId(100L);
            existingUnread.setIsRead(false);

            when(notificationRepository.findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse(
                    1L, 2L, NotificationType.LIKE, "POST", 100L))
                    .thenReturn(Optional.of(existingUnread));
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
            when(webSocketHandler.hasActiveSession(1L)).thenReturn(false);

            notificationService.createNotification(1L, 2L, NotificationType.LIKE, "POST", 100L, "Test");

            verify(notificationRepository).delete(existingUnread);
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("COMMENT 类型不触发去重逻辑")
        void shouldNotDeduplicateForComment() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
            when(webSocketHandler.hasActiveSession(1L)).thenReturn(false);

            notificationService.createNotification(1L, 2L, NotificationType.COMMENT, "POST", 100L, "Nice!");

            verify(notificationRepository, never())
                    .findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse(
                            anyLong(), anyLong(), any(), anyString(), anyLong());
        }

        @Test
        @DisplayName("无活跃 WebSocket 连接 → 不推送")
        void shouldNotPushWhenNoActiveSession() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
            when(webSocketHandler.hasActiveSession(1L)).thenReturn(false);

            notificationService.createNotification(1L, 2L, NotificationType.LIKE, "POST", 100L, "Test");

            verify(webSocketHandler, never()).sendToUser(anyLong(), anyString());
        }

        @Test
        @DisplayName("actor 不存在时 → actorNickname 为 Unknown User")
        void shouldHandleMissingActor() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            when(userRepository.findById(2L)).thenReturn(Optional.empty());
            when(webSocketHandler.hasActiveSession(1L)).thenReturn(false);

            NotificationResponse result = notificationService.createNotification(
                    1L, 2L, NotificationType.LIKE, "POST", 100L, "Test");

            assertNotNull(result);
            assertEquals("Unknown User", result.getActorNickname());
        }
    }

    // ==================== getNotifications ====================

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("分页查询 — 返回通知列表 + hasMore + totalElements")
        void shouldReturnPagedNotifications() {
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<Notification> page = new PageImpl<>(List.of(sampleNotification), pageRequest, 25);

            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                    .thenReturn(page);
            when(userRepository.findById(2L)).thenReturn(Optional.of(actor));

            NotificationListResponse result = notificationService.getNotifications(1L, 0, 20);

            assertEquals(1, result.getNotifications().size());
            assertEquals(true, result.isHasMore());
            assertEquals(25, result.getTotalElements());
        }

        @Test
        @DisplayName("空列表 — hasMore=false, totalElements=0")
        void shouldReturnEmptyList() {
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<Notification> page = new PageImpl<>(List.of(), pageRequest, 0);

            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                    .thenReturn(page);

            NotificationListResponse result = notificationService.getNotifications(1L, 0, 20);

            assertEquals(0, result.getNotifications().size());
            assertEquals(false, result.isHasMore());
            assertEquals(0, result.getTotalElements());
        }
    }

    // ==================== markAsRead ====================

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("正常标记 — 属于当前用户 → isRead=true")
        void shouldMarkAsRead() {
            when(notificationRepository.findByUuid(notificationUuid))
                    .thenReturn(Optional.of(sampleNotification));

            notificationService.markAsRead(notificationUuid.toString(), 1L);

            assertEquals(true, sampleNotification.getIsRead());
            verify(notificationRepository).save(sampleNotification);
        }

        @Test
        @DisplayName("UUID 不存在 → 抛出 EntityNotFoundException")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findByUuid(any(UUID.class)))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> notificationService.markAsRead(UUID.randomUUID().toString(), 1L));
        }

        @Test
        @DisplayName("不属于当前用户 → 抛出 ForbiddenException")
        void shouldThrowWhenNotOwner() {
            when(notificationRepository.findByUuid(notificationUuid))
                    .thenReturn(Optional.of(sampleNotification));

            assertThrows(ForbiddenException.class,
                    () -> notificationService.markAsRead(notificationUuid.toString(), 999L));
        }
    }

    // ==================== markAllAsRead ====================

    @Test
    @DisplayName("markAllAsRead — 委托 Repository 批量更新")
    void shouldDelegateToRepository() {
        when(notificationRepository.markAllAsRead(1L)).thenReturn(5);

        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsRead(1L);
    }

    // ==================== getUnreadCount ====================

    @Test
    @DisplayName("getUnreadCount — 返回正确计数")
    void shouldReturnUnreadCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(7L);

        UnreadCountResponse result = notificationService.getUnreadCount(1L);

        assertEquals(7L, result.getCount());
    }

    // ==================== cleanupOldNotifications ====================

    @Test
    @DisplayName("cleanupOldNotifications — 删除 14 天前的通知")
    void shouldDeleteOldNotifications() {
        when(notificationRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(42);

        int deleted = notificationService.cleanupOldNotifications();

        assertEquals(42, deleted);
        verify(notificationRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
