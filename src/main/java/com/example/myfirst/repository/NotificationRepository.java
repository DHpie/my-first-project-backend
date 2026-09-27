package com.example.myfirst.repository;

import com.example.myfirst.entity.Notification;
import com.example.myfirst.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByUuid(UUID uuid);

    /**
     按用户 ID 分页查询通知（时间倒序）
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     统计用户未读通知数量
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     批量标记用户所有未读通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     查找重复点赞通知（用于去重）
     */
    Optional<Notification> findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse(
            Long userId, Long actorId, NotificationType type, String targetType, Long targetId);

    /**
     删除 createdAt 早于指定时间的通知记录
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);

    /**
     查找用户指定时间之后的未读通知（用于断线重连后补发）
     */
    List<Notification> findByUserIdAndIsReadFalseAndCreatedAtAfter(
            Long userId, LocalDateTime after);
}
