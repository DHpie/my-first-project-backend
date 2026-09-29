package com.example.myfirst.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "ai_conversation", indexes = {
        @Index(name = "idx_ai_conv_user_id", columnList = "user_id")
})
public class AiConversation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title = "AI Travel Assistant";

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}
