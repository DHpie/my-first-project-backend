package com.example.myfirst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读通知计数响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    private long count;
}
