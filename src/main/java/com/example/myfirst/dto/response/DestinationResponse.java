package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DestinationResponse {

    private Long id;
    private String cityName;
    private String slug;
    private String highlight;
    private String coverImageUrl;
    private String popularityTag;
}
