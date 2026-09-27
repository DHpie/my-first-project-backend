package com.example.myfirst.common;

/**
 * 被屏蔽时抛出的异常（HTTP 403）
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
