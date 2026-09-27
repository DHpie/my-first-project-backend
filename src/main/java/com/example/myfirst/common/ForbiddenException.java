package com.example.myfirst.common;

/**
 * 权限不足异常 —— 被屏蔽或访问非本人资源时抛出（HTTP 403）
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
