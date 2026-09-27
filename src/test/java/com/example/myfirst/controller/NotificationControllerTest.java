package com.example.myfirst.controller;

import com.example.myfirst.dto.response.NotificationListResponse;
import com.example.myfirst.dto.response.NotificationResponse;
import com.example.myfirst.dto.response.UnreadCountResponse;
import com.example.myfirst.service.NotificationService;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@DisplayName("NotificationController API 契约测试")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private static final String USER_HEADER = "X-User-Id";
    private static final Long USER_ID = 1L;

    // ==================== GET /api/notifications ====================

    @Nested
    @DisplayName("GET /api/notifications")
    class GetNotifications {

        @Test
        @DisplayName("正常请求 → 200 + Result<T> 格式")
        void shouldReturnNotificationList() throws Exception {
            NotificationResponse notification = new NotificationResponse(
                    UUID.randomUUID().toString(), "LIKE",
                    "https://i.pravatar.cc/40?u=test", "Sarah",
                    "liked", "Test snippet",
                    LocalDateTime.of(2026, 9, 27, 10, 0),
                    false, false, 100L);

            NotificationListResponse listResponse = new NotificationListResponse(
                    List.of(notification), false, 1);

            when(notificationService.getNotifications(eq(USER_ID), eq(0), eq(20)))
                    .thenReturn(listResponse);

            mockMvc.perform(get("/api/notifications")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.notifications").isArray())
                    .andExpect(jsonPath("$.data.notifications", hasSize(1)))
                    .andExpect(jsonPath("$.data.notifications[0].type").value("LIKE"))
                    .andExpect(jsonPath("$.data.notifications[0].actorNickname").value("Sarah"))
                    .andExpect(jsonPath("$.data.notifications[0].actionText").value("liked"))
                    .andExpect(jsonPath("$.data.notifications[0].targetId").value(100))
                    .andExpect(jsonPath("$.data.hasMore").value(false))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("分页参数传递 — page=1, size=10")
        void shouldPassPaginationParams() throws Exception {
            NotificationListResponse emptyResponse = new NotificationListResponse(List.of(), false, 0);
            when(notificationService.getNotifications(USER_ID, 1, 10)).thenReturn(emptyResponse);

            mockMvc.perform(get("/api/notifications")
                            .header(USER_HEADER, USER_ID)
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notifications", hasSize(0)))
                    .andExpect(jsonPath("$.data.hasMore").value(false));
        }

        @Test
        @DisplayName("缺少 X-User-Id → 400 Bad Request")
        void shouldReturn400WhenMissingUserHeader() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== PUT /api/notifications/{uuid}/read ====================

    @Nested
    @DisplayName("PUT /api/notifications/{uuid}/read")
    class MarkAsRead {

        @Test
        @DisplayName("正常标记 → 200 + Result<Void>")
        void shouldMarkAsRead() throws Exception {
            String uuid = UUID.randomUUID().toString();
            doNothing().when(notificationService).markAsRead(uuid, USER_ID);

            mockMvc.perform(put("/api/notifications/" + uuid + "/read")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"));
        }

        @Test
        @DisplayName("UUID 不存在 → 404 (EntityNotFoundException)")
        void shouldReturn404WhenNotFound() throws Exception {
            String uuid = UUID.randomUUID().toString();
            doThrow(new jakarta.persistence.EntityNotFoundException("Notification not found"))
                    .when(notificationService).markAsRead(eq(uuid), eq(USER_ID));

            mockMvc.perform(put("/api/notifications/" + uuid + "/read")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("不属于当前用户 → 403 Forbidden")
        void shouldReturn403WhenNotOwner() throws Exception {
            String uuid = UUID.randomUUID().toString();
            doThrow(new com.example.myfirst.common.ForbiddenException("Notification does not belong to current user"))
                    .when(notificationService).markAsRead(eq(uuid), eq(USER_ID));

            mockMvc.perform(put("/api/notifications/" + uuid + "/read")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    // ==================== PUT /api/notifications/read-all ====================

    @Nested
    @DisplayName("PUT /api/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("正常请求 → 200")
        void shouldMarkAllAsRead() throws Exception {
            doNothing().when(notificationService).markAllAsRead(USER_ID);

            mockMvc.perform(put("/api/notifications/read-all")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ==================== GET /api/notifications/unread-count ====================

    @Nested
    @DisplayName("GET /api/notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("正常请求 → 200 + {count: N}")
        void shouldReturnUnreadCount() throws Exception {
            when(notificationService.getUnreadCount(USER_ID))
                    .thenReturn(new UnreadCountResponse(5));

            mockMvc.perform(get("/api/notifications/unread-count")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.count").value(5));
        }

        @Test
        @DisplayName("零未读 → count=0")
        void shouldReturnZeroCount() throws Exception {
            when(notificationService.getUnreadCount(USER_ID))
                    .thenReturn(new UnreadCountResponse(0));

            mockMvc.perform(get("/api/notifications/unread-count")
                            .header(USER_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.count").value(0));
        }
    }
}
