package com.example.myfirst.service;

import com.example.myfirst.dto.response.NotificationListResponse;
import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.dto.response.UnreadCountResponse;
import com.example.myfirst.entity.NotificationType;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     创建通知（含重复点赞去重逻辑）
     @param userId        通知接收者 ID
     @param actorId       操作者 ID
     @param type          通知类型
     @param targetType    目标类型
     @param targetId      目标 ID
     @param contentSnippet 内容片段
     @return 创建的通知 DTO（如果因去重被替换，返回新通知）
     */
    NotificationResponse createNotification(Long userId, Long actorId, NotificationType type,
                                            String targetType, Long targetId, String contentSnippet);

    /**
     分页获取用户通知列表
     @param userId 用户 ID
     @param page   页码（从 0 开始）
     @param size   每页大小
     @return 分页通知列表
     */
    NotificationListResponse getNotifications(Long userId, int page, int size);

    /**
     标记单条通知为已读
     @param notificationUuid 通知 UUID
     @param userId           当前用户 ID
     */
    void markAsRead(String notificationUuid, Long userId);

    /**
     批量标记用户所有未读通知为已读
     @param userId 用户 ID
     */
    void markAllAsRead(Long userId);

    /**
     获取用户未读通知计数
     @param userId 用户 ID
     @return 未读计数
     */
    UnreadCountResponse getUnreadCount(Long userId);

    /**
     清理过期通知（删除 createdAt 早于 14 天前的记录）
     @return 删除的通知数量
     */
    int cleanupOldNotifications();
}
