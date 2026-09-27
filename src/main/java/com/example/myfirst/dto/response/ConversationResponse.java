package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long conversationId;
    private Long otherUserId;
    private String otherUserAvatar;
    private String otherUserNickname;
    private String lastMessageSnippet;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
}
