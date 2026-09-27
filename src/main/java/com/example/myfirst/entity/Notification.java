package com.example.myfirst.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 通知实体 —— 记录用户互动通知（点赞/评论/回复）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    /** 通知接收者 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 操作者 ID */
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    /** 通知类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** 目标类型（如 POST、COMMENT） */
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    /** 目标 ID */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 内容片段（截断后的文本） */
    @Column(name = "content_snippet", length = 500)
    private String contentSnippet;

    /** 是否已读 */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /** 原始内容是否已删除 */
    @Column(name = "is_content_deleted", nullable = false)
    private Boolean isContentDeleted = false;
}
