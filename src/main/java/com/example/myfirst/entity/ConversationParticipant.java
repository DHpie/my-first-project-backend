package com.example.myfirst.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {

    @EmbeddedId
    private ConversationParticipantId id;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    @Data
    @NoArgsConstructor
    @Embeddable
    public static class ConversationParticipantId implements Serializable {

        @Column(name = "conversation_id", nullable = false)
        private Long conversationId;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        public ConversationParticipantId(Long conversationId, Long userId) {
            this.conversationId = conversationId;
            this.userId = userId;
        }
    }

    public ConversationParticipant(Long conversationId, Long userId) {
        this.id = new ConversationParticipantId(conversationId, userId);
        this.unreadCount = 0;
    }
}
