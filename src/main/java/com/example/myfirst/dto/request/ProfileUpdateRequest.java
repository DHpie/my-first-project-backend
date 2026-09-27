package com.example.myfirst.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProfileUpdateRequest {

    @Size(min = 1, max = 30, message = "Nickname must be between 1 and 30 characters")
    private String nickname;

    @Size(max = 200, message = "Bio must be 200 characters or less")
    private String bio;

    private List<String> interestTags;
}
