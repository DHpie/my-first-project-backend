package com.example.myfirst.controller;

import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.service.PostInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostInteractionController.class)
@DisplayName("PostInteractionController API 契约测试")
class PostInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostInteractionService postInteractionService;

    private static final String USER_HEADER = "X-User-Id";
    private static final Long USER_ID = 2L;

    private NotificationResponse sampleNotification() {
        return new NotificationResponse(
                UUID.randomUUID().toString(), "LIKE",
                "https://i.pravatar.cc/40?u=liwei", "Li Wei",
                "liked", "3 Days in Chengdu...",
                LocalDateTime.of(2026, 9, 27, 10, 0),
                false, false, 1L);
    }

    // ==================== POST /api/posts/{postId}/like ====================

    @Nested
    @DisplayName("POST /api/posts/{postId}/like")
    class LikePost {

        @Test
        @DisplayName("点赞成功 → 200 + 通知 DTO")
        void shouldLikePostAndReturnNotification() throws Exception {
            when(postInteractionService.likePost(USER_ID, 1L)).thenReturn(sampleNotification());

            mockMvc.perform(post("/api/posts/1/like")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.type").value("LIKE"))
                    .andExpect(jsonPath("$.data.targetId").value(1));
        }

        @Test
        @DisplayName("自己点赞自己帖子 → 200 + data=null")
        void shouldReturnNullWhenSelfLike() throws Exception {
            when(postInteractionService.likePost(USER_ID, 2L)).thenReturn(null);

            mockMvc.perform(post("/api/posts/2/like")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("帖子不存在 → 404")
        void shouldReturn404WhenPostNotFound() throws Exception {
            when(postInteractionService.likePost(USER_ID, 999L))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Post not found"));

            mockMvc.perform(post("/api/posts/999/like")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== POST /api/posts/{postId}/comment ====================

    @Nested
    @DisplayName("POST /api/posts/{postId}/comment")
    class CommentOnPost {

        @Test
        @DisplayName("评论成功 → 200 + COMMENT 通知")
        void shouldCommentAndReturnNotification() throws Exception {
            NotificationResponse commentNotification = new NotificationResponse(
                    UUID.randomUUID().toString(), "COMMENT",
                    "https://i.pravatar.cc/40?u=sarah", "Sarah",
                    "commented on", "Great post!",
                    LocalDateTime.of(2026, 9, 27, 11, 0),
                    false, false, 1L);

            when(postInteractionService.commentOnPost(eq(USER_ID), eq(1L), eq("Great post!")))
                    .thenReturn(commentNotification);

            mockMvc.perform(post("/api/posts/1/comment")
                            .header(USER_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "Great post!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("COMMENT"))
                    .andExpect(jsonPath("$.data.contentSnippet").value("Great post!"));
        }
    }

    // ==================== POST /api/comments/{commentId}/reply ====================

    @Nested
    @DisplayName("POST /api/comments/{commentId}/reply")
    class ReplyToComment {

        @Test
        @DisplayName("回复成功 → 200 + REPLY 通知")
        void shouldReplyAndReturnNotification() throws Exception {
            NotificationResponse replyNotification = new NotificationResponse(
                    UUID.randomUUID().toString(), "REPLY",
                    "https://i.pravatar.cc/40?u=marco", "Marco",
                    "replied to", "Thanks for the info!",
                    LocalDateTime.of(2026, 9, 27, 12, 0),
                    false, false, 50L);

            when(postInteractionService.replyToComment(eq(USER_ID), eq(1L), eq(50L), eq("Thanks!")))
                    .thenReturn(replyNotification);

            mockMvc.perform(post("/api/posts/comments/50/reply")
                            .header(USER_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("commentOwnerId", "1", "content", "Thanks!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("REPLY"));
        }
    }
}
