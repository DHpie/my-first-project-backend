package com.example.myfirst.common;

/**
 * 频率限制异常（HTTP 429）
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
