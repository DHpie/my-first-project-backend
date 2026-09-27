package com.example.myfirst.repository;

import com.example.myfirst.entity.Notification;
import com.example.myfirst.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("NotificationRepository JPA 集成测试 (H2)")
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    // 直接使用 Long ID，不依赖 User 实体（避免 H2 保留字 'user' 冲突）
    private static final Long USER_1_ID = 1L;
    private static final Long USER_2_ID = 2L;

    @BeforeEach
    void setUp() {
        // 清理之前的测试数据
        notificationRepository.deleteAll();
        entityManager.flush();
    }

    private Notification createNotification(Long userId, Long actorId, NotificationType type,
                                            String targetType, Long targetId, boolean isRead) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setActorId(actorId);
        n.setType(type);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setContentSnippet("Test content");
        n.setIsRead(isRead);
        n.setIsContentDeleted(false);
        return n;
    }
    // ==================== 分页查询 ====================

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc — 按时间倒序分页")
    void shouldReturnNotificationsInDescendingOrder() {
        // 创建 3 条通知
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 1L, false));
        entityManager.flush();

        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.COMMENT, "POST", 1L, false));
        entityManager.flush();

        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.REPLY, "COMMENT", 1L, false));
        entityManager.flush();

        // 查询
        Page<Notification> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(USER_1_ID, PageRequest.of(0, 2));

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertTrue(page.hasNext());
        // 最新的在前
        assertTrue(page.getContent().get(0).getCreatedAt()
                .isAfter(page.getContent().get(1).getCreatedAt())
                || page.getContent().get(0).getCreatedAt()
                .isEqual(page.getContent().get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc — 只返回指定用户的通知")
    void shouldOnlyReturnNotificationsForUser() {
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 1L, false));
        entityManager.persist(createNotification(USER_2_ID, USER_1_ID,
                NotificationType.LIKE, "POST", 2L, false));
        entityManager.flush();

        Page<Notification> user1Page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(USER_1_ID, PageRequest.of(0, 20));
        Page<Notification> user2Page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(USER_2_ID, PageRequest.of(0, 20));

        assertEquals(1, user1Page.getTotalElements());
        assertEquals(1, user2Page.getTotalElements());
    }

    // ==================== 未读计数 ====================

    @Test
    @DisplayName("countByUserIdAndIsReadFalse — 只统计未读")
    void shouldCountOnlyUnread() {
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 1L, false));
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.COMMENT, "POST", 1L, false));
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.REPLY, "COMMENT", 1L, true)); // 已读
        entityManager.flush();

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(USER_1_ID);

        assertEquals(2, unreadCount);
    }

    // ==================== 批量标记已读 ====================

    @Test
    @DisplayName("markAllAsRead — 批量标记用户所有未读通知")
    void shouldMarkAllAsRead() {
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 1L, false));
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.COMMENT, "POST", 1L, false));
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.REPLY, "COMMENT", 1L, true)); // 已读
        entityManager.flush();

        int updated = notificationRepository.markAllAsRead(USER_1_ID);

        assertEquals(2, updated);
        assertEquals(0, notificationRepository.countByUserIdAndIsReadFalse(USER_1_ID));
    }

    // ==================== 重复点赞去重查询 ====================

    @Test
    @DisplayName("findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse — 查找未读重复点赞")
    void shouldFindUnreadDuplicateLike() {
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 100L, false));
        entityManager.flush();

        Optional<Notification> found = notificationRepository
                .findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse(
                        USER_1_ID, USER_2_ID, NotificationType.LIKE, "POST", 100L);

        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("已读点赞不被去重查询找到")
    void shouldNotFindReadLike() {
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 100L, true)); // 已读
        entityManager.flush();

        Optional<Notification> found = notificationRepository
                .findByUserIdAndActorIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalse(
                        USER_1_ID, USER_2_ID, NotificationType.LIKE, "POST", 100L);

        assertFalse(found.isPresent());
    }

    // ==================== 过期清理 ====================

    @Test
    @DisplayName("deleteByCreatedAtBefore — 删除过期通知")
    void shouldDeleteOldNotifications() {
        // 创建一条通知并手动设置 createdAt 为 30 天前
        Notification oldNotification = createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 1L, false);
        entityManager.persist(oldNotification);
        entityManager.flush();

        entityManager.getEntityManager()
                .createQuery("UPDATE Notification n SET n.createdAt = :dt WHERE n.id = :id")
                .setParameter("dt", LocalDateTime.now().minusDays(30))
                .setParameter("id", oldNotification.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // 创建一条新的通知
        entityManager.persist(createNotification(USER_1_ID, USER_2_ID,
                NotificationType.COMMENT, "POST", 2L, false));
        entityManager.flush();

        int deleted = notificationRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(14));

        assertEquals(1, deleted);
        assertEquals(1, notificationRepository.count());
    }

    // ==================== findByUuid ====================

    @Test
    @DisplayName("findByUuid — 通过 UUID 查找通知")
    void shouldFindByUuid() {
        Notification n = createNotification(USER_1_ID, USER_2_ID,
                NotificationType.LIKE, "POST", 1L, false);
        entityManager.persist(n);
        entityManager.flush();
        entityManager.clear();

        Notification saved = notificationRepository.findById(n.getId()).orElseThrow();
        Optional<Notification> found = notificationRepository.findByUuid(saved.getUuid());

        assertTrue(found.isPresent());
        assertEquals(n.getId(), found.get().getId());
    }
}
