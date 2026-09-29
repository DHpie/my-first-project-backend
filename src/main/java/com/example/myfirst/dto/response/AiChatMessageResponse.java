package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageResponse {
    private Long id;
    private String role;
    private String content;
    private String createdAt;
}
