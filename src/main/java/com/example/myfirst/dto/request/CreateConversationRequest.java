package com.example.myfirst.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotNull(message = "otherUserId is required")
    private Long otherUserId;
}
