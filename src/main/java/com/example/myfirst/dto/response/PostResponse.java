package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String authorName;
    private String authorAvatarUrl;
    private String title;
    private String excerpt;
    private Integer likeCount;
}
