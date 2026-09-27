package com.example.myfirst.service.impl;

import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.entity.NotificationType;
import com.example.myfirst.service.NotificationService;
import com.example.myfirst.service.PostInteractionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 帖子互动服务实现
 * <p>
 * 当前使用模拟帖子→所有者映射（与 PostController 的 FEATURED_POSTS 一致），
 * 后续引入 Post/Comment 实体后替换为数据库查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final NotificationService notificationService;

    /** 模拟帖子 ID → 所有者用户 ID 映射（与 PostController.FEATURED_POSTS 一致） */
    private static final Map<Long, Long> POST_OWNER_MAP = Map.of(
            1L, 1L,  // Li Wei
            2L, 2L,  // Sarah Chen
            3L, 3L   // Marco Rossi
    );

    /** 模拟帖子 ID → 标题映射（用于 contentSnippet 截断） */
    private static final Map<Long, String> POST_TITLE_MAP = Map.of(
            1L, "3 Days in Chengdu — A Food Lover's Guide",
            2L, "Hidden Gems Along the Li River",
            3L, "First-Time Visitor Tips for Xi'an"
    );

    private static final int SNIPPET_MAX_LENGTH = 100;

    @Override
    @Transactional
    public NotificationResponse likePost(Long actorId, Long postId) {
        Long postOwnerId = getPostOwner(postId);
        String title = getPostTitle(postId);
        String snippet = truncate(title, SNIPPET_MAX_LENGTH);

        log.info("User {} liked post {} (owner={})", actorId, postId, postOwnerId);
        return notificationService.createNotification(
                postOwnerId, actorId, NotificationType.LIKE, "POST", postId, snippet);
    }

    @Override
    @Transactional
    public NotificationResponse commentOnPost(Long actorId, Long postId, String content) {
        Long postOwnerId = getPostOwner(postId);
        String snippet = truncate(content, SNIPPET_MAX_LENGTH);

        log.info("User {} commented on post {} (owner={})", actorId, postId, postOwnerId);
        return notificationService.createNotification(
                postOwnerId, actorId, NotificationType.COMMENT, "POST", postId, snippet);
    }

    @Override
    @Transactional
    public NotificationResponse replyToComment(Long actorId, Long commentOwnerId, Long commentId, String content) {
        String snippet = truncate(content, SNIPPET_MAX_LENGTH);

        log.info("User {} replied to comment {} (owner={})", actorId, commentId, commentOwnerId);
        return notificationService.createNotification(
                commentOwnerId, actorId, NotificationType.REPLY, "COMMENT", commentId, snippet);
    }

    private Long getPostOwner(Long postId) {
        Long ownerId = POST_OWNER_MAP.get(postId);
        if (ownerId == null) {
            throw new EntityNotFoundException("Post not found with id: " + postId);
        }
        return ownerId;
    }

    private String getPostTitle(Long postId) {
        return POST_TITLE_MAP.getOrDefault(postId, "Post #" + postId);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
