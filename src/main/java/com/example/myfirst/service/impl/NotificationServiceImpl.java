package com.example.myfirst.service.impl;

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
import com.example.myfirst.service.NotificationService;
import com.example.myfirst.websocket.NotificationWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationResponse createNotification(Long userId, Long actorId, NotificationType type,
                                                   String targetType, Long targetId, String contentSnippet) {
        // 不给自己发通知
        if (userId.equals(actorId)) {
            return null;
        }

        // 重复点赞去重：如果是 LIKE 类型，查找是否有未读的同类型通知
        if (type == NotificationType.LIKE) {
            Optional<Notification> existingUnread = notificationRepository
                    .findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse(
                            userId, actorId, type, targetType, targetId);

            existingUnread.ifPresent(notification -> {
                notificationRepository.delete(notification);
                log.debug("Deleted duplicate unread LIKE notification: userId={}, actorId={}, targetId={}",
                        userId, actorId, targetId);
            });
        }

        // 创建新通知
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setContentSnippet(contentSnippet);
        notification.setIsRead(false);
        notification.setIsContentDeleted(false);

        Notification saved = notificationRepository.save(notification);

        // 查询操作者信息用于 DTO 转换
        User actor = userRepository.findById(actorId).orElse(null);
        NotificationResponse response = NotificationMapper.toResponse(saved, actor);

        // WebSocket 实时推送
        pushNotificationToUser(userId, response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(notification -> {
                    User actor = userRepository.findById(notification.getActorId()).orElse(null);
                    return NotificationMapper.toResponse(notification, actor);
                })
                .collect(Collectors.toList());

        return new NotificationListResponse(
                notifications,
                notificationPage.hasNext(),
                notificationPage.getTotalElements()
        );
    }

    @Override
    @Transactional
    public void markAsRead(String notificationUuid, Long userId) {
        UUID uuid = UUID.fromString(notificationUuid);
        Notification notification = notificationRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with uuid: " + notificationUuid));

        // 验证通知属于当前用户
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Notification does not belong to current user");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public int cleanupOldNotifications() {
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        int deleted = notificationRepository.deleteByCreatedAtBefore(fourteenDaysAgo);
        log.info("Cleaned up {} notifications older than 14 days", deleted);
        return deleted;
    }

    /**
     通过 WebSocket 向用户推送通知
     */
    private void pushNotificationToUser(Long userId, NotificationResponse response) {
        if (!webSocketHandler.hasActiveSession(userId)) {
            log.debug("No active WebSocket session for userId={}, skipping push", userId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(response);
            webSocketHandler.sendToUser(userId, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for userId={}: {}", userId, e.getMessage());
        }
    }
}
