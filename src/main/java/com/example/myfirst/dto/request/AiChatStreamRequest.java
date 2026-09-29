package com.example.myfirst.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiChatStreamRequest {

    @NotBlank(message = "Message field is required")
    @Size(max = 500, message = "Message exceeds maximum length of 500 characters")
    private String message;

    private Long conversationId;
}
