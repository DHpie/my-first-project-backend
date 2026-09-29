package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatHistoryResponse {
    private Long conversationId;
    private List<AiChatMessageResponse> messages;
    private boolean hasMore;
}
