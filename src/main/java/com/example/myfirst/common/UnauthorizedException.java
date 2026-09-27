package com.example.myfirst.common;

/**
 * 未登录异常（HTTP 401）
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
