package com.example.myfirst.service;

import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.entity.NotificationType;
import com.example.myfirst.service.impl.PostInteractionServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostInteractionServiceImpl 单元测试")
class PostInteractionServiceImplTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostInteractionServiceImpl postInteractionService;

    // ==================== likePost ====================

    @Test
    @DisplayName("点赞帖子 → 调用 createNotification(LIKE) 并传入帖子所有者 ID")
    void shouldCreateLikeNotificationForPostOwner() {
        NotificationResponse expected = new NotificationResponse();
        when(notificationService.createNotification(
                eq(1L), eq(2L), eq(NotificationType.LIKE), eq("POST"), eq(1L), anyString()))
                .thenReturn(expected);

        NotificationResponse result = postInteractionService.likePost(2L, 1L);

        assertSame(expected, result);
        verify(notificationService).createNotification(
                eq(1L), eq(2L), eq(NotificationType.LIKE), eq("POST"), eq(1L),
                eq("3 Days in Chengdu — A Food Lover's Guide"));
    }

    @Test
    @DisplayName("点赞自己的帖子 → createNotification 返回 null（自操作过滤）")
    void shouldReturnNullWhenLikingOwnPost() {
        when(notificationService.createNotification(
                eq(2L), eq(2L), eq(NotificationType.LIKE), eq("POST"), eq(2L), anyString()))
                .thenReturn(null);

        NotificationResponse result = postInteractionService.likePost(2L, 2L);

        assertNull(result);
    }

    @Test
    @DisplayName("帖子不存在 → 抛出 EntityNotFoundException")
    void shouldThrowWhenPostNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> postInteractionService.likePost(2L, 999L));
    }

    // ==================== commentOnPost ====================

    @Test
    @DisplayName("评论帖子 → 调用 createNotification(COMMENT) 并传入评论内容片段")
    void shouldCreateCommentNotification() {
        NotificationResponse expected = new NotificationResponse();
        when(notificationService.createNotification(
                eq(1L), eq(2L), eq(NotificationType.COMMENT), eq("POST"), eq(1L), eq("Nice guide!")))
                .thenReturn(expected);

        NotificationResponse result = postInteractionService.commentOnPost(2L, 1L, "Nice guide!");

        assertSame(expected, result);
        verify(notificationService).createNotification(
                1L, 2L, NotificationType.COMMENT, "POST", 1L, "Nice guide!");
    }

    // ==================== replyToComment ====================

    @Test
    @DisplayName("回复评论 → 调用 createNotification(REPLY) 并传入评论所有者 ID")
    void shouldCreateReplyNotification() {
        NotificationResponse expected = new NotificationResponse();
        when(notificationService.createNotification(
                eq(3L), eq(2L), eq(NotificationType.REPLY), eq("COMMENT"), eq(50L), eq("Thanks!")))
                .thenReturn(expected);

        NotificationResponse result = postInteractionService.replyToComment(2L, 3L, 50L, "Thanks!");

        assertSame(expected, result);
        verify(notificationService).createNotification(
                3L, 2L, NotificationType.REPLY, "COMMENT", 50L, "Thanks!");
    }
}
