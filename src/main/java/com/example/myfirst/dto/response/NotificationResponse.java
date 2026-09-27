package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单条通知响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String type;
    private String actorAvatar;
    private String actorNickname;
    private String actionText;
    private String contentSnippet;
    private LocalDateTime timestamp;
    private Boolean isRead;
    private Boolean isContentDeleted;
    /** 目标帖子 ID，用于点击跳转 */
    private Long targetId;
}
