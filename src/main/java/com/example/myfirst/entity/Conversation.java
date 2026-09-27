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
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conv_user_pair", columnNames = {"user1_id", "user2_id"})
}, indexes = {
        @Index(name = "idx_conv_user1", columnList = "user1_id"),
        @Index(name = "idx_conv_user2", columnList = "user2_id"),
        @Index(name = "idx_conv_last_msg", columnList = "last_message_at")
})
public class Conversation extends BaseEntity {

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
}
