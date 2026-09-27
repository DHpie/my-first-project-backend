package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private String id;
    private String username;
    private String email;
    private String nickname;
    private String bio;
    private String avatarUrl;
    private List<String> interestTags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
